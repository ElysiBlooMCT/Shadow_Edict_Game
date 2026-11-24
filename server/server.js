// =====================================================================
//  Shadow Edict: Ám Lệnh - Socket.IO server
//  - Phòng 1v1 / Co-op (2 người) và 5v5 (tối đa 10 người) + đồng bộ trận đấu
//  - v1.5: gửi "serverInfo" khi kết nối; chủ phòng bắt đầu trận bằng "startGame"
//  - Hệ thống bạn bè: định danh theo máy (deviceId), tìm kiếm, kết bạn,
//    trạng thái online, mời vào trận. Dữ liệu lưu vào users.json.
// =====================================================================
const express = require('express');
const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { Server } = require('socket.io');

const app = express();
const server = http.createServer(app);

const io = new Server(server, {
    cors: { origin: '*' },
    pingInterval: 2000,   // kiểm tra ping mỗi 2 giây (mặc định 25s quá chậm với game)
    pingTimeout: 5000,    // 5 giây không phản hồi -> coi như mất kết nối
    maxHttpBufferSize: 64 * 1024, // gói lớn nhất là voice ~1,1 KB; chặn gói rác cỡ MB làm nghẽn server
});

// Server chạy liên tục: một lỗi bất ngờ chỉ được ghi log, không làm sập cả phòng chơi của mọi người
process.on('uncaughtException', (err) => console.error('❗ Loi khong mong muon:', err));
process.on('unhandledRejection', (err) => console.error('❗ Promise bi tu choi:', err));

const ROOM_MODES = ['1v1', 'coop', '5v5'];
const MODE_NAMES = { '1v1': 'Đấu 1v1', coop: 'Hợp tác', '5v5': 'Đại chiến 5v5' };
const normMode = (m) => (ROOM_MODES.includes(m) ? m : '1v1');

// Giới hạn tần suất gói đồng bộ mỗi kết nối (game gửi tối đa ~20 gói vị trí/giây)
const RATE_LIMIT_PER_SEC = 60;
function allowPacket(socket, kind) {
    const now = Date.now();
    const r = socket._rate || (socket._rate = {});
    const c = r[kind] || (r[kind] = { t: now, n: 0 });
    if (now - c.t >= 1000) { c.t = now; c.n = 0; }
    return ++c.n <= RATE_LIMIT_PER_SEC;
}

const hasOwn = (obj, key) => Object.prototype.hasOwnProperty.call(obj, key);
const makeNullDict = (src) => {
    const out = Object.create(null);
    if (src && typeof src === 'object') {
        Object.keys(src).forEach((k) => { out[k] = src[k]; });
    }
    return out;
};

// ---------------------------------------------------------------------
//  LƯU TRỮ NGƯỜI CHƠI (users.json)
// ---------------------------------------------------------------------
const DATA_FILE = process.env.AL_DATA_FILE || path.join(__dirname, 'users.json');
let db = { users: Object.create(null), deviceIndex: Object.create(null) }; // users[userId] = {...}; deviceIndex[hash(deviceId)] = userId

function loadDb() {
    try {
        if (fs.existsSync(DATA_FILE)) {
            const raw = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
            db = {
                users: makeNullDict(raw && raw.users),
                deviceIndex: makeNullDict(raw && raw.deviceIndex),
            };
            // Dữ liệu từ server bản cũ: bảo đảm mọi user đều đủ các trường bắt buộc
            Object.keys(db.users).forEach((id) => {
                const u = db.users[id];
                if (!u || typeof u !== 'object') { delete db.users[id]; return; }
                u.userId = u.userId || id;
                u.name = cleanName(u.name || 'Chiến Thần');
                u.level = Number.isFinite(Number(u.level)) && Number(u.level) >= 1 ? Math.floor(Number(u.level)) : 1;
                u.gold = Number.isFinite(Number(u.gold)) && Number(u.gold) >= 0 ? Math.floor(Number(u.gold)) : 500;
                u.matches = Number.isFinite(Number(u.matches)) && Number(u.matches) >= 0 ? Math.floor(Number(u.matches)) : 0;
                u.wins = Number.isFinite(Number(u.wins)) && Number(u.wins) >= 0 ? Math.min(u.matches, Math.floor(Number(u.wins))) : 0;
                ['friends', 'incoming', 'outgoing'].forEach((k) => { if (!Array.isArray(u[k])) u[k] = []; });
            });
        }
    } catch (e) {
        console.error('Khong doc duoc', DATA_FILE, e.message);
    }
    console.log(`📂 Da nap ${Object.keys(db.users).length} nguoi choi`);
}

let saveTimer = null;
let isSaving = false;
let saveQueued = false;
function flushDbAsync() {
    if (isSaving) {
        saveQueued = true;
        return;
    }
    isSaving = true;
    saveQueued = false;
    const tmp = DATA_FILE + '.tmp';
    const payload = JSON.stringify(db);
    fs.writeFile(tmp, payload, (err) => {
        if (err) {
            console.error('Loi luu du lieu:', err.message);
            isSaving = false;
            if (saveQueued) flushDbAsync();
            return;
        }
        fs.rename(tmp, DATA_FILE, (rErr) => {
            if (rErr) console.error('Loi doi ten file du lieu:', rErr.message);
            isSaving = false;
            if (saveQueued) flushDbAsync();
        });
    });
}
function saveDb() {
    // Ghi gộp sau 1s, tuần tự hóa các lần ghi đĩa để không ghi đè file tạm cùng lúc
    if (saveTimer) return;
    saveTimer = setTimeout(() => {
        saveTimer = null;
        flushDbAsync();
    }, 1000);
}
function flushDbSync() {
    if (saveTimer) {
        clearTimeout(saveTimer);
        saveTimer = null;
    }
    try {
        const tmp = DATA_FILE + '.tmp';
        fs.writeFileSync(tmp, JSON.stringify(db));
        fs.renameSync(tmp, DATA_FILE);
    } catch (e) {
        console.error('Loi luu dong bo khi tat server:', e.message);
    }
}
process.on('SIGINT', () => { flushDbSync(); process.exit(0); });
process.on('SIGTERM', () => { flushDbSync(); process.exit(0); });

// Chỉ lưu dạng băm của deviceId (không lưu định danh máy gốc)
const hashDevice = (deviceId) => crypto.createHash('sha256').update(String(deviceId)).digest('hex');

// Cấp mã AL-XXXX duy nhất (4 số; nếu hết chỗ thì tự lên 5-6 số)
function allocateUserId(preferred) {
    if (typeof preferred === 'string' && /^AL-\d{4,6}$/.test(preferred) && !hasOwn(db.users, preferred)) return preferred;
    for (let digits = 4; digits <= 6; digits++) {
        const min = 10 ** (digits - 1), span = 9 * min;
        for (let i = 0; i < 50; i++) {
            const id = 'AL-' + (min + Math.floor(Math.random() * span));
            if (!hasOwn(db.users, id)) return id;
        }
    }
    return 'AL-' + Date.now();
}

const cleanName = (n) => String(n || '').replace(/[\r\n\t]/g, ' ').trim().slice(0, 16) || 'Chiến Thần';
// So khớp tên không phân biệt hoa/thường và dấu tiếng Việt ("chien" tìm được "Chiến Thần")
const fold = (s) => String(s || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase();

// ---------------------------------------------------------------------
//  TRẠNG THÁI ONLINE (trong RAM)
// ---------------------------------------------------------------------
const online = Object.create(null);   // userId -> { socketId, status, roomCode, mode }

function presenceOf(userId) {
    const p = hasOwn(online, userId) ? online[userId] : null;
    return p ? { status: p.status, roomCode: p.roomCode, mode: p.mode }
             : { status: 'OFFLINE', roomCode: '', mode: '' };
}

function emitToUser(userId, event, payload) {
    const p = hasOwn(online, userId) ? online[userId] : null;
    if (p) io.to(p.socketId).emit(event, payload);
}

function friendView(userId) {
    const u = hasOwn(db.users, userId) ? db.users[userId] : null;
    return { userId, name: u ? u.name : '???', level: u ? u.level : 1, ...presenceOf(userId) };
}

function sendFriendsList(userId) {
    const u = hasOwn(db.users, userId) ? db.users[userId] : null;
    if (!u) return;
    emitToUser(userId, 'friendsList', u.friends.filter((f) => hasOwn(db.users, f)).map(friendView));
}

function broadcastPresence(userId) {
    const u = hasOwn(db.users, userId) ? db.users[userId] : null;
    if (!u) return;
    const payload = { userId, ...presenceOf(userId) };
    u.friends.forEach((f) => emitToUser(f, 'friendPresenceUpdate', payload));
}

// ---------------------------------------------------------------------
//  PHÒNG CHƠI
// ---------------------------------------------------------------------
const rooms = Object.create(null);

function newRoomCode() {
    for (let i = 0; i < 100; i++) {
        const code = Math.floor(1000 + Math.random() * 9000).toString();
        if (!hasOwn(rooms, code)) return code;
    }
    return Date.now().toString().slice(-6);
}

function startRoomMatch(code, room) {
    console.log(`🔥 Phong [${code}] bat dau tran dau! (${room.players.length} nguoi)`);
    room.players.forEach((p) => {
        p.ready = false; // trận sau phải bấm sẵn sàng lại
        if (p.userId && hasOwn(online, p.userId) && online[p.userId].socketId === p.id) {
            online[p.userId].status = 'IN_GAME';
            broadcastPresence(p.userId);
        }
    });
    // player1/player2: giữ trường của giao thức cũ cho game bản cũ
    io.to(code).emit('startGame', { players: room.players, selectedMap: room.selectedMap || 1,
        player1: room.players[0], player2: room.players[1] });
}

function leaveCurrentRoom(socket) {
    const code = socket.roomCode;
    if (!code) return;
    socket.leave(code);
    socket.roomCode = null;
    if (socket.userId && hasOwn(online, socket.userId) && online[socket.userId].socketId === socket.id) {
        online[socket.userId].status = 'ONLINE';
        online[socket.userId].roomCode = '';
        online[socket.userId].mode = '';
        broadcastPresence(socket.userId);
    }
    const room = hasOwn(rooms, code) ? rooms[code] : null;
    if (!room) return;
    const leavingPlayer = room.players.find((p) => p.id === socket.id);
    room.players = room.players.filter((p) => p.id !== socket.id);
    if (room.players.length === 0) {
        delete rooms[code];
        console.log(`🧹 Phong [${code}] da giai tan`);
    } else {
        room.players.forEach((p) => { p.ready = false; });
        io.to(code).emit('playerLeft', { id: socket.id, player: leavingPlayer, room: room });
        io.to(code).emit('updateRoomState', room);
    }
}

loadDb();

// Phiên bản giao thức: game v1.5+ đọc "serverInfo" để biết máy chủ đã hỗ trợ startGame/phòng 5v5 chưa.
const SERVER_VERSION = '1.5.0';
const PROTOCOL = 2;

io.on('connection', (socket) => {
    socket.emit('serverInfo', { version: SERVER_VERSION, protocol: PROTOCOL });
    console.log('⚡ Nguoi choi ket noi:', socket.id);

    // ============================ BẠN BÈ ============================

    // Đăng ký/đăng nhập tự động theo máy: cùng deviceId -> luôn nhận lại cùng mã AL-XXXX
    socket.on('registerUser', (data) => {
        data = (data && typeof data === 'object') ? data : {};
        if (typeof data.deviceId !== 'string' || !data.deviceId.trim()) {
            return socket.emit('errorMsg', 'Thieu deviceId - hay cap nhat game!');
        }
        const dHash = hashDevice(data.deviceId.trim());
        let userId = hasOwn(db.deviceIndex, dHash) ? db.deviceIndex[dHash] : null;
        if (!userId || !hasOwn(db.users, userId)) {
            userId = allocateUserId(data.userId);
            db.users[userId] = {
                userId, name: cleanName(data.name), level: 1, gold: 500, matches: 0, wins: 0,
                favoriteHero: 'Vân Tiêu',
                friends: [], incoming: [], outgoing: [], createdAt: Date.now(),
            };
            db.deviceIndex[dHash] = userId;
            console.log(`🆕 Nguoi choi moi ${userId} (${db.users[userId].name})`);
        } else if (typeof data.name === 'string' && data.name.trim()) {
            db.users[userId].name = cleanName(data.name); // đổi tên trong game -> cập nhật server
        }
        const u = db.users[userId];
        const safeNum = (v, min, max) => (typeof v === 'number' && Number.isFinite(v) && v >= min && v <= max) ? Math.floor(v) : null;
        const lvl = safeNum(data.level, 1, 999);
        if (lvl !== null && lvl > (u.level || 1)) u.level = lvl;
        const gld = safeNum(data.gold, 0, 100000000);
        if (gld !== null && gld > (u.gold || 0)) u.gold = gld;
        const mch = safeNum(data.matches, 0, 1000000);
        if (mch !== null) u.matches = Math.max(u.matches || 0, mch);
        const wns = safeNum(data.wins, 0, 1000000);
        if (wns !== null) u.wins = Math.min(u.matches || 0, Math.max(u.wins || 0, wns));
        if (typeof data.favoriteHero === 'string' && data.favoriteHero.trim()) {
            u.favoriteHero = data.favoriteHero.replace(/[\r\n\t]/g, ' ').trim().slice(0, 24);
        }
        u.lastSeen = Date.now();
        saveDb();

        // Nếu socket này đổi tài khoản trên cùng kết nối thì dọn bản ghi online cũ
        const prevUid = socket.userId;
        if (prevUid && prevUid !== userId && hasOwn(online, prevUid) && online[prevUid].socketId === socket.id) {
            delete online[prevUid];
            broadcastPresence(prevUid);
        }

        // Bảo toàn trạng thái IN_ROOM/IN_GAME nếu người chơi đang ở trong phòng mà đổi tên
        const activeRoom = (socket.roomCode && hasOwn(rooms, socket.roomCode)) ? rooms[socket.roomCode] : null;
        const prevOnline = (hasOwn(online, userId) && online[userId].socketId === socket.id) ? online[userId] : null;
        socket.userId = userId;
        online[userId] = activeRoom
            ? {
                socketId: socket.id,
                status: (prevOnline && prevOnline.status === 'IN_GAME') ? 'IN_GAME' : 'IN_ROOM',
                roomCode: socket.roomCode,
                mode: activeRoom.mode,
            }
            : { socketId: socket.id, status: 'ONLINE', roomCode: '', mode: '' };

        socket.emit('userRegistered', { userId, name: u.name, gold: u.gold, level: u.level });
        sendFriendsList(userId);
        u.incoming.forEach((rid) => {
            if (hasOwn(db.users, rid)) socket.emit('friendRequestReceived', { requesterId: rid, requesterName: db.users[rid].name });
        });
        broadcastPresence(userId);
    });

    const me = () => (socket.userId && hasOwn(db.users, socket.userId)) ? db.users[socket.userId] : null;

    socket.on('getFriendsList', () => { if (me()) sendFriendsList(socket.userId); });

    socket.on('getUserProfile', (targetUserId) => {
        targetUserId = String(targetUserId || '').trim().toUpperCase();
        const u = hasOwn(db.users, targetUserId) ? db.users[targetUserId] : null;
        if (!u) {
            return socket.emit('userProfileData', { userId: targetUserId, found: false });
        }
        const pres = presenceOf(targetUserId);
        socket.emit('userProfileData', {
            found: true,
            userId: u.userId,
            name: u.name,
            level: u.level || 1,
            gold: u.gold || 500,
            matches: u.matches || 0,
            wins: u.wins || 0,
            favoriteHero: u.favoriteHero || 'Vân Tiêu',
            status: pres.status,
            roomCode: pres.roomCode,
            mode: pres.mode
        });
    });

    socket.on('searchUsers', (query) => {
        const u = me();
        if (!u) return socket.emit('searchResults', []);
        const q = String(query || '').trim().toLowerCase();
        const qf = fold(q);
        if (!q) return socket.emit('searchResults', []);
        const results = Object.values(db.users)
            .filter((x) => x.userId !== u.userId &&
                (x.userId.toLowerCase() === q || x.userId.toLowerCase() === 'al-' + q ||
                 fold(x.name).includes(qf)))
            .slice(0, 20)
            .map((x) => ({
                userId: x.userId, name: x.name, level: x.level,
                isFriend: u.friends.includes(x.userId),
                isPending: u.outgoing.includes(x.userId),
            }));
        socket.emit('searchResults', results);
    });

    function makeFriends(a, b) {
        const ua = hasOwn(db.users, a) ? db.users[a] : null;
        const ub = hasOwn(db.users, b) ? db.users[b] : null;
        if (!ua || !ub) return;
        [ua, ub].forEach((x) => {
            x.incoming = x.incoming.filter((id) => id !== a && id !== b);
            x.outgoing = x.outgoing.filter((id) => id !== a && id !== b);
        });
        if (!ua.friends.includes(b)) ua.friends.push(b);
        if (!ub.friends.includes(a)) ub.friends.push(a);
        saveDb();
        emitToUser(a, 'friendRequestAccepted', { userId: b });
        emitToUser(b, 'friendRequestAccepted', { userId: a });
        sendFriendsList(a);
        sendFriendsList(b);
    }

    socket.on('sendFriendRequest', (targetId) => {
        const u = me();
        const tid = String(targetId || '').trim().toUpperCase();
        const t = hasOwn(db.users, tid) ? db.users[tid] : null;
        if (!u || !t || t.userId === u.userId) return;
        if (u.friends.includes(t.userId)) return;
        if (u.incoming.includes(t.userId)) return makeFriends(u.userId, t.userId); // 2 bên cùng gửi -> thành bạn
        if (!u.outgoing.includes(t.userId)) u.outgoing.push(t.userId);
        if (!t.incoming.includes(u.userId)) t.incoming.push(u.userId);
        saveDb();
        emitToUser(t.userId, 'friendRequestReceived', { requesterId: u.userId, requesterName: u.name });
    });

    socket.on('acceptFriendRequest', (requesterId) => {
        const u = me();
        const rid = String(requesterId || '').trim().toUpperCase();
        if (!u || !u.incoming.includes(rid)) return;
        makeFriends(u.userId, rid);
    });

    socket.on('declineFriendRequest', (requesterId) => {
        const u = me();
        const rid = String(requesterId || '').trim().toUpperCase();
        const r = hasOwn(db.users, rid) ? db.users[rid] : null;
        if (!u) return;
        u.incoming = u.incoming.filter((id) => id !== rid);
        if (r) r.outgoing = r.outgoing.filter((id) => id !== u.userId);
        saveDb();
    });

    socket.on('removeFriend', (friendId) => {
        const u = me();
        const fid = String(friendId || '').trim().toUpperCase();
        const f = hasOwn(db.users, fid) ? db.users[fid] : null;
        if (!u) return;
        u.friends = u.friends.filter((id) => id !== fid);
        if (f) f.friends = f.friends.filter((id) => id !== u.userId);
        saveDb();
        emitToUser(u.userId, 'friendRemoved', { userId: fid });
        emitToUser(fid, 'friendRemoved', { userId: u.userId });
        sendFriendsList(u.userId);
        sendFriendsList(fid);
    });

    socket.on('updatePresence', (data) => {
        const u = me();
        if (!u || !hasOwn(online, u.userId) || online[u.userId].socketId !== socket.id) return;
        data = (data && typeof data === 'object') ? data : {};
        const p = online[u.userId];
        p.status = ['ONLINE', 'IN_ROOM', 'IN_GAME'].includes(data.status) ? data.status : 'ONLINE';
        p.roomCode = String(data.roomCode || '');
        p.mode = String(data.mode || '');
        broadcastPresence(u.userId);
    });

    socket.on('inviteToMatch', (data) => {
        const u = me();
        data = (data && typeof data === 'object') ? data : {};
        const tid = String(data.targetUserId || '').trim().toUpperCase();
        if (!u || !u.friends.includes(tid)) return; // chỉ mời được bạn bè
        emitToUser(tid, 'matchInviteReceived', {
            inviterId: u.userId, inviterName: u.name,
            roomCode: String(data.roomCode || ''), mode: String(data.mode || '1v1'),
        });
    });

    // ============================ PHÒNG ============================

    socket.on('getRoomState', () => {
        const code = socket.roomCode;
        if (code && hasOwn(rooms, code)) {
            socket.emit('updateRoomState', rooms[code]);
        }
    });

    socket.on('createRoom', (data) => {
        data = (data && typeof data === 'object') ? data : {};
        leaveCurrentRoom(socket);
        const roomCode = newRoomCode();
        const mode = normMode(data.type);
        const pName = data.name ? cleanName(data.name) : (socket.userId && hasOwn(db.users, socket.userId) ? db.users[socket.userId].name : 'Chiến Thần');
        const pTeam = data.team ? ((data.team === 2) ? 2 : 1) : 1;
        const pChar = data.charId ? ((data.charId === 2) ? 2 : 1) : 1;
        const pUid = data.userId ? String(data.userId).trim().toUpperCase() : (socket.userId || '');
        const selMap = parseInt(data.selectedMap, 10);
        rooms[roomCode] = {
            mode,
            selectedMap: (selMap >= 1 && selMap <= 3) ? selMap : 1,
            players: [{ id: socket.id, userId: pUid, name: pName, team: pTeam, charId: pChar, ready: false }]
        };
        socket.join(roomCode);
        socket.roomCode = roomCode;
        if (socket.userId && hasOwn(online, socket.userId) && online[socket.userId].socketId === socket.id) {
            online[socket.userId].status = 'IN_ROOM';
            online[socket.userId].roomCode = roomCode;
            online[socket.userId].mode = mode;
            broadcastPresence(socket.userId);
        }
        socket.emit('roomCreated', { roomCode, playerIndex: 1 });
        io.to(roomCode).emit('updateRoomState', rooms[roomCode]);
        console.log(`🏰 Phong [${roomCode}] (${mode}) da duoc tao boi ${socket.id} (${pName})`);
    });

    socket.on('joinRoom', (roomCode, data) => {
        data = (data && typeof data === 'object') ? data : {};
        roomCode = String(roomCode || '').trim();
        const room = hasOwn(rooms, roomCode) ? rooms[roomCode] : null;
        if (!room) return socket.emit('errorMsg', 'Mã phòng không tồn tại!');

        const pUid = data.userId ? String(data.userId).trim().toUpperCase() : (socket.userId || '');
        // Đã ở trong phòng (VD: quay lại sảnh sau trận hoặc kết nối lại bằng socket mới trước khi socket cũ timeout)
        let idx = room.players.findIndex((p) => p.id === socket.id);
        if (idx < 0 && pUid) {
            idx = room.players.findIndex((p) => p.userId && p.userId === pUid);
            if (idx >= 0) {
                const oldSocketId = room.players[idx].id;
                const oldSock = io.sockets.sockets.get(oldSocketId);
                if (oldSock && oldSock.id !== socket.id) {
                    oldSock.leave(roomCode);
                    if (oldSock.roomCode === roomCode) oldSock.roomCode = null;
                }
                if (socket.roomCode && socket.roomCode !== roomCode) {
                    leaveCurrentRoom(socket);
                }
                room.players[idx].id = socket.id;
                socket.join(roomCode);
                socket.roomCode = roomCode;
            }
        }
        if (idx >= 0) {
            if (data.team) room.players[idx].team = (data.team === 2) ? 2 : 1;
            if (data.charId) room.players[idx].charId = (data.charId === 2) ? 2 : 1;
            if (data.name) room.players[idx].name = cleanName(data.name);
            if (pUid) room.players[idx].userId = pUid;
            room.players.forEach((p) => { p.ready = false; });
            if (socket.userId && hasOwn(online, socket.userId) && online[socket.userId].socketId === socket.id) {
                online[socket.userId].status = 'IN_ROOM';
                online[socket.userId].roomCode = roomCode;
                online[socket.userId].mode = room.mode;
                broadcastPresence(socket.userId);
            }
            socket.emit('roomJoined', { roomCode, playerIndex: idx + 1 });
            io.to(roomCode).emit('updateRoomState', room);
            return;
        }
        // Không cho vào nhầm chế độ (VD nhập mã phòng 5v5 ở sảnh 1v1): hai máy sẽ vào hai chế độ khác nhau
        if (ROOM_MODES.includes(data.type) && data.type !== room.mode) {
            return socket.emit('errorMsg', `Mã ${roomCode} là phòng ${MODE_NAMES[room.mode] || room.mode}, hãy vào đúng sảnh!`);
        }
        const maxPlayers = (room.mode === '5v5') ? 10 : 2;
        if (room.players.length >= maxPlayers) return socket.emit('errorMsg', `Phòng đã đầy (${maxPlayers} người)!`);

        leaveCurrentRoom(socket);
        const pName = data.name ? cleanName(data.name) : (socket.userId && hasOwn(db.users, socket.userId) ? db.users[socket.userId].name : 'Chiến Thần');
        let pTeam = data.team ? ((data.team === 2) ? 2 : 1) : (room.players.length % 2 === 0 ? 1 : 2);
        if (room.mode === '5v5') {
            // Cân bằng phe khi vào phòng: game luôn gửi phe đang chọn (mặc định Xanh) nên trước đây
            // mọi người đều rơi vào cùng phe Xanh. Nếu phe yêu cầu đã đông hơn thì xếp sang phe còn lại
            // (người chơi vẫn đổi phe được bằng Q/E -> "selectTeam").
            const blue = room.players.filter((p) => p.team === 1).length;
            const red = room.players.length - blue;
            if (pTeam === 1 && blue > red) pTeam = 2;
            else if (pTeam === 2 && red > blue) pTeam = 1;
        }
        const pChar = data.charId ? ((data.charId === 2) ? 2 : 1) : 2;
        const newPlayer = { id: socket.id, userId: pUid, name: pName, team: pTeam, charId: pChar, ready: false };
        room.players.push(newPlayer);
        socket.join(roomCode);
        socket.roomCode = roomCode;
        if (socket.userId && hasOwn(online, socket.userId) && online[socket.userId].socketId === socket.id) {
            online[socket.userId].status = 'IN_ROOM';
            online[socket.userId].roomCode = roomCode;
            online[socket.userId].mode = room.mode;
            broadcastPresence(socket.userId);
        }
        socket.emit('roomJoined', { roomCode, playerIndex: room.players.length });
        io.to(roomCode).emit('playerJoined', { playerIndex: room.players.length, player: newPlayer, room: room });
        io.to(roomCode).emit('updateRoomState', room);
        console.log(`⚔️ ${socket.id} (${pName}) da tham gia phong [${roomCode}] (${room.players.length}/${maxPlayers})`);
    });

    socket.on('leaveRoom', () => leaveCurrentRoom(socket));

    socket.on('selectCharacter', (charId) => {
        const room = (socket.roomCode && hasOwn(rooms, socket.roomCode)) ? rooms[socket.roomCode] : null;
        if (!room) return;
        const player = room.players.find((p) => p.id === socket.id);
        if (player) {
            player.charId = charId === 2 ? 2 : 1;
            io.to(socket.roomCode).emit('updateRoomState', room);
        }
    });

    socket.on('selectTeam', (team) => {
        const room = (socket.roomCode && hasOwn(rooms, socket.roomCode)) ? rooms[socket.roomCode] : null;
        if (!room) return;
        const player = room.players.find((p) => p.id === socket.id);
        if (player) {
            player.team = (team === 2) ? 2 : 1;
            io.to(socket.roomCode).emit('updateRoomState', room);
        }
    });

    socket.on('selectMap', (mapId) => {
        const room = (socket.roomCode && hasOwn(rooms, socket.roomCode)) ? rooms[socket.roomCode] : null;
        if (!room) return;
        if (room.players.length > 0 && room.players[0].id !== socket.id) return;
        const m = parseInt(mapId, 10);
        room.selectedMap = (m >= 1 && m <= 3) ? m : 1;
        io.to(socket.roomCode).emit('updateRoomState', room);
        console.log(`🗺️ Phong [${socket.roomCode}] chon Ban do: ${room.selectedMap}`);
    });

    socket.on('startGame', () => {
        const room = (socket.roomCode && hasOwn(rooms, socket.roomCode)) ? rooms[socket.roomCode] : null;
        if (!room) return;
        // Chỉ chủ phòng (người đầu tiên trong players) mới có quyền phát lệnh bắt đầu trận đấu
        if (room.players.length > 0 && room.players[0].id !== socket.id) {
            socket.emit('errorMsg', 'Chỉ chủ phòng mới có quyền bắt đầu trận đấu!');
            return;
        }
        // Chế độ 1v1 / Hợp tác phải đủ 2 người mới được bắt đầu (tránh race condition khi khách vừa thoát)
        if (room.mode !== '5v5' && room.players.length < 2) {
            socket.emit('errorMsg', 'Cần đủ 2 người chơi để bắt đầu trận đấu!');
            return;
        }
        // Kiểm tra xem tất cả người chơi khác (từ index 1 trở đi) đã bấm Sẵn sàng chưa
        if (room.players.length > 1) {
            const allGuestsReady = room.players.slice(1).every((p) => p.ready);
            if (!allGuestsReady) {
                socket.emit('errorMsg', 'Tất cả người chơi trong phòng phải sẵn sàng mới có thể bắt đầu!');
                return;
            }
        }
        startRoomMatch(socket.roomCode, room);
    });

    socket.on('toggleReady', () => {
        const room = (socket.roomCode && hasOwn(rooms, socket.roomCode)) ? rooms[socket.roomCode] : null;
        if (!room) return;
        const player = room.players.find((p) => p.id === socket.id);
        if (!player) return;
        player.ready = !player.ready;
        io.to(socket.roomCode).emit('updateRoomState', room);
        // Tương thích game bản cũ (v1.4 trở về trước): bản cũ không có nút "Bắt đầu" của chủ phòng,
        // trận tự bắt đầu khi MỌI người (kể cả chủ phòng) bấm Sẵn sàng. Game v1.5 không bao giờ để
        // chủ phòng ở trạng thái Sẵn sàng nên nhánh này chỉ chạy với máy cài bản cũ.
        if (room.players.length >= 2 && room.players.every((p) => p.ready)) {
            startRoomMatch(socket.roomCode, room);
        }
    });

    // ===================== ĐỒNG BỘ TRẬN ĐẤU =====================
    socket.on('playerMove', (data) => {
        if (!socket.roomCode || !allowPacket(socket, 'move')) return;
        // volatile: mạng nghẽn thì bỏ gói tọa độ cũ, luôn hiển thị vị trí mới nhất
        socket.volatile.to(socket.roomCode).emit('enemyMove', data);
    });

    socket.on('playerAction', (actionData) => {
        if (!socket.roomCode || !actionData || typeof actionData !== 'object' || !allowPacket(socket, 'action')) return;
        // Đồng bộ đòn đánh, kỹ năng, chat văn bản và gói âm thanh voice chat trong phòng
        if (actionData.type === 'voice') {
            if (typeof actionData.pcm !== 'string' || actionData.pcm.length > 4500) return;
            const safeVoice = {
                type: 'voice',
                sender: String(actionData.sender || 'Đồng đạo').replace(/[\r\n]/g, ' ').trim().slice(0, 24),
                pcm: actionData.pcm,
            };
            socket.volatile.to(socket.roomCode).emit('enemyAction', safeVoice);
        } else if (actionData.type === 'chat') {
            if (typeof actionData.text !== 'string') return;
            const cleanText = actionData.text.replace(/[\r\n]/g, ' ').trim().slice(0, 140);
            if (!cleanText) return;
            const safeChat = {
                type: 'chat',
                sender: String(actionData.sender || 'Đồng đạo').replace(/[\r\n]/g, ' ').trim().slice(0, 24),
                team: actionData.team === 2 ? 2 : 1,
                text: cleanText,
            };
            socket.to(socket.roomCode).emit('enemyAction', safeChat);
        } else {
            socket.to(socket.roomCode).emit('enemyAction', actionData);
        }
    });

    // ===================== NGẮT KẾT NỐI =====================
    socket.on('disconnect', () => {
        console.log('❌ Nguoi choi ngat ket noi:', socket.id);
        // Bản cũ xóa cả phòng khi 1 người thoát; giờ chỉ rời phòng, người còn lại vẫn giữ phòng
        leaveCurrentRoom(socket);
        const uid = socket.userId;
        if (uid && hasOwn(online, uid) && online[uid].socketId === socket.id) {
            delete online[uid];
            if (hasOwn(db.users, uid)) { db.users[uid].lastSeen = Date.now(); saveDb(); }
            broadcastPresence(uid);
        }
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 Server Shadow Edict dang chay tai port ${PORT}...`);
});
