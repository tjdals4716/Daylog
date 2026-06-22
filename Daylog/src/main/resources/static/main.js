// ==========================================
// 1. JWT 인증 및 공통 유틸 로직
// ==========================================
const API_BASE_URL = (window.APP_CONFIG && window.APP_CONFIG.BACKEND_BASE) || 'http://localhost:8086';
const TOKEN_KEY = 'accessToken';

function getToken() { return localStorage.getItem(TOKEN_KEY) || ''; }

function decodeJwt(token) {
    try {
        const part = token.split('.')[1];
        if (!part) return null;
        let b64 = part.replace(/-/g, '+').replace(/_/g, '/');
        while (b64.length % 4) b64 += '=';
        const json = decodeURIComponent(
            atob(b64).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')
        );
        return JSON.parse(json);
    } catch { return null; }
}

function getUid() {
    const t = getToken();
    if (t) {
        const p = decodeJwt(t);
        if (p && (p.sub || p.uid || p.username)) return p.sub || p.uid || p.username;
    }
    try {
        const cu = JSON.parse(localStorage.getItem('currentUser') || 'null');
        if (cu && cu.uid) return cu.uid;
    } catch (_) {}
    return '';
}

function isTokenValid() {
    const t = getToken();
    if (!t) return false;
    const p = decodeJwt(t);
    if (!p) return false;
    if (p.exp && Date.now() >= p.exp * 1000) return false;
    return true;
}

function authHeaders(withJson = false) {
    const h = {};
    if (withJson) h['Content-Type'] = 'application/json';
    const t = getToken();
    if (t) h['Authorization'] = 'Bearer ' + t;
    return h;
}

let _authRedirecting = false;
function redirectToLogin(msg) {
    if (_authRedirecting) return;
    _authRedirecting = true;
    alert(msg || '세션이 만료되었거나 존재하지 않습니다. 다시 로그인해주세요.');
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem('currentUser');
    localStorage.removeItem('auth');
    location.href = 'login.html';
}

function requireAuthOrRedirect() {
    if (!isTokenValid()) { redirectToLogin(); return false; }
    return true;
}

async function handleResponse(res) {
    if (res.status === 401 || res.status === 403) {
        redirectToLogin('세션이 만료되었습니다. 다시 로그인해주세요.');
        throw new Error('인증이 만료되었습니다');
    }
    if (!res.ok) {
        const text = await res.text().catch(() => '');
        if (/jwt|token|expired|signature|malformed|unauthor|forbidden|authentication/i.test(text)) {
            redirectToLogin('세션이 만료되었습니다. 다시 로그인해주세요.');
            throw new Error('인증이 만료되었습니다');
        }
        throw new Error(text || (res.status + ' ' + res.statusText));
    }
    if (res.status === 204) return null;
    return res.json();
}

// ==========================================
// 2. 메인 앱 로직
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    // 페이지 진입 시 가장 먼저 인증 체크
    if (!requireAuthOrRedirect()) return;

    let map = null;
    let selectedFile = null;
    let currentLatLng = null;
    let isWaitingForMapClick = false;
    let mapClickListener = null;
    let memoryList = []; // 메모리 데이터를 보관할 배열

    const currentUid = getUid();

    // --- 디데이 계산 ---
    calculateDDay(new Date("2026-05-09"));

    // --- 로그아웃 ---
    document.getElementById('btn-logout').addEventListener('click', (e) => {
        e.preventDefault();
        if (confirm('정말 로그아웃 하시겠어요?')) redirectToLogin('로그아웃 되었습니다.');
    });

    // --- 탭 전환 ---
    const navItems = document.querySelectorAll('.nav-item');
    const tabContents = document.querySelectorAll('.tab-content');
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            navItems.forEach(nav => nav.classList.remove('active'));
            item.classList.add('active');
            const targetTab = item.getAttribute('data-tab');
            tabContents.forEach(tab => {
                tab.style.display = (tab.id === targetTab) ? 'block' : 'none';
            });
            if(targetTab === 'tab-map' && map) naver.maps.Event.trigger(map, 'resize');
        });
    });

    // --- 네이버 지도 초기화 ---
    if (window.APP_CONFIG && window.APP_CONFIG.NAVER_MAP_CLIENT_ID) {
        const script = document.createElement('script');
        script.src = `https://openapi.map.naver.com/openapi/v3/maps.js?submodules=geocoder&ncpKeyId=${window.APP_CONFIG.NAVER_MAP_CLIENT_ID}`;
        script.async = true;
        script.onload = () => initMap();
        document.head.appendChild(script);
    }

    function initMap() {
        map = new naver.maps.Map('naver-map', {
            center: new naver.maps.LatLng(37.5665, 126.9780),
            zoom: 12
        });
        loadMemoriesFromServer();
    }

    // --- 사진 업로드 & 위치 지정 ---
    const fileInput = document.getElementById('memory-file');
    const mapGuideBanner = document.getElementById('map-guide-banner');

    if (fileInput) {
        fileInput.addEventListener('change', async (e) => {
            const file = e.target.files[0];
            if (!file) return;

            selectedFile = file;

            // 미리보기
            const reader = new FileReader();
            reader.onload = (ev) => {
                const preview = document.getElementById('image-preview');
                preview.src = ev.target.result;
                preview.classList.remove('hidden');
            };
            reader.readAsDataURL(file);

            try {
                const gps = await exifr.gps(file);
                if (gps && gps.latitude && gps.longitude) {
                    // 메타데이터 O
                    currentLatLng = { lat: gps.latitude, lng: gps.longitude };
                    document.getElementById('location-status-badge').innerText = "📍 사진 위치가 자동으로 설정되었습니다!";
                    document.getElementById('location-status-badge').className = "location-badge success";
                    openMemoryModal();
                } else {
                    // 메타데이터 X -> 지도 클릭 가이드 활성화
                    isWaitingForMapClick = true;
                    mapGuideBanner.classList.remove('hidden'); // 안내 배너 노출

                    if (mapClickListener) naver.maps.Event.removeListener(mapClickListener);
                    mapClickListener = naver.maps.Event.addListener(map, 'click', (event) => {
                        if (!isWaitingForMapClick) return;

                        currentLatLng = { lat: event.coord.lat(), lng: event.coord.lng() };
                        document.getElementById('location-status-badge').innerText = "🎯 지도에서 지정한 위치로 설정되었습니다";
                        document.getElementById('location-status-badge').className = "location-badge manual";

                        isWaitingForMapClick = false;
                        mapGuideBanner.classList.add('hidden'); // 안내 배너 숨김
                        naver.maps.Event.removeListener(mapClickListener);
                        openMemoryModal();
                    });
                }
            } catch (error) {
                alert('사진 분석 중 오류가 발생했습니다. 지도에서 위치를 설정해주세요.');
            }
        });
    }

    // --- 폼 제출 (백엔드 전송) ---
    const memoryForm = document.getElementById('memory-form');
    if (memoryForm) {
        memoryForm.addEventListener('submit', (e) => {
            e.preventDefault();
            if (!requireAuthOrRedirect()) return;

            if (!currentLatLng) { alert('위치 정보가 없습니다.'); return; }

            const memoryDTO = {
                title: document.getElementById('memory-title').value,
                content: document.getElementById('memory-content').value,
                lat: currentLatLng.lat,
                lng: currentLatLng.lng,
                createdAt: new Date(document.getElementById('memory-date').value).toISOString()
            };

            const formData = new FormData();
            formData.append("uid", currentUid);
            formData.append("memoryData", JSON.stringify(memoryDTO));
            if (selectedFile) formData.append("mediaData", selectedFile);

            fetch(`${API_BASE_URL}/api/memories`, {
                method: 'POST',
                headers: authHeaders(false),
                body: formData
            })
                .then(handleResponse)
                .then(() => {
                    alert('소중한 기억이 기록되었습니다! 🤎');
                    closeMemoryModal();
                    loadMemoriesFromServer();
                })
                .catch(err => {
                    console.error(err);
                });
        });
    }

    // --- 데이터 불러오기 및 렌더링 ---
    function loadMemoriesFromServer() {
        if (!requireAuthOrRedirect()) return;

        fetch(`${API_BASE_URL}/api/memories/${currentUid}`, {
            headers: authHeaders(true)
        })
            .then(handleResponse)
            .then(memories => {
                memoryList = memories; // 상세 조회를 위해 저장
                document.getElementById('total-memories-count').innerText = memories.length;
                const timelineFeed = document.getElementById('timeline-feed');
                timelineFeed.innerHTML = '';

                const sortedMemories = [...memories].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

                sortedMemories.forEach(memory => {
                    // 1. 지도 마커 추가
                    if (map && memory.lat && memory.lng) {
                        const markerHtml = memory.mediaURL
                            ? `<div class="custom-marker"><img src="${memory.mediaURL}"></div>`
                            : `<div style="font-size:24px;">💖</div>`;

                        const marker = new naver.maps.Marker({
                            position: new naver.maps.LatLng(memory.lat, memory.lng),
                            map: map,
                            icon: { content: markerHtml, anchor: new naver.maps.Point(22, 22) }
                        });

                        // 마커 클릭 시 상세 모달 오픈
                        naver.maps.Event.addListener(marker, 'click', () => openDetailModal(memory));
                    }

                    // 2. 타임라인 카드 추가
                    const formattedDate = memory.createdAt ? memory.createdAt.substring(0,10).replace(/-/g, '.') : '';
                    const card = document.createElement('div');
                    card.classList.add('memory-card');
                    card.innerHTML = `
                    <div class="card-header">
                        <span class="card-date">${formattedDate}</span>
                        <h4 class="card-title">${escapeHtml(memory.title)}</h4>
                    </div>
                    <p class="card-text">${escapeHtml(memory.content)}</p>
                `;
                    // 타임라인 클릭 시 상세 모달 오픈
                    card.addEventListener('click', () => openDetailModal(memory));
                    timelineFeed.appendChild(card);
                });
            })
            .catch(err => console.error("데이터 로드 실패:", err));
    }
});

// ==========================================
// 3. UI 제어 유틸리티
// ==========================================
function openMemoryModal() {
    const modal = document.getElementById('memory-modal');
    modal.classList.remove('hidden');
    document.getElementById('memory-date').value = new Date().toISOString().substring(0, 10);
}

function closeMemoryModal() {
    document.getElementById('memory-modal').classList.add('hidden');
    document.getElementById('memory-form').reset();
    document.getElementById('image-preview').classList.add('hidden');
    // 사용자가 취소했을 경우를 대비해 지도 클릭 리스너 해제
    document.getElementById('map-guide-banner').classList.add('hidden');
}

// 상세 조회 모달 열기
function openDetailModal(memory) {
    const formattedDate = memory.createdAt ? memory.createdAt.substring(0,10).replace(/-/g, '.') : '';
    document.getElementById('detail-date').innerText = formattedDate;
    document.getElementById('detail-title').innerText = memory.title;
    document.getElementById('detail-text').innerText = memory.content;

    const imgEl = document.getElementById('detail-image');
    if (memory.mediaURL) {
        imgEl.src = memory.mediaURL;
        imgEl.classList.remove('hidden');
    } else {
        imgEl.classList.add('hidden');
        imgEl.src = "";
    }

    document.getElementById('detail-modal').classList.remove('hidden');
}

function closeDetailModal() {
    document.getElementById('detail-modal').classList.add('hidden');
}

function calculateDDay(start) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    start.setHours(0, 0, 0, 0);
    const diffDays = Math.floor((today.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
    const ddayCountEl = document.getElementById('dday-count');
    if (ddayCountEl) ddayCountEl.innerText = diffDays;
}

function escapeHtml(text) {
    if(!text) return '';
    return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
}