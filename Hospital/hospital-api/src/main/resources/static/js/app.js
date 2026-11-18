        const API_BASE = '/api';
        let hospitals = [];
        let doctors = [];
        let currentUser = null;
        let currentRole = 'admin';
        let currentUserId = null;
        let currentAdminRole = null;
        let currentHospitalId = null;
        let currentRegRole = 'patient';
        let adminHospitalPage = 0;
        let adminAdminPage = 0;
        const LIST_PAGE_SIZE = 8;
        const listPages = {};
        let hospitalsView = [];
        let doctorsView = [];
        let appointmentsView = [];
        let adminDepartmentsView = [];
        let adminDoctorsView = [];
        let adminScheduleView = [];
        let currentScheduleDoctorId = null;
        let dutyRulesView = [];
        let dutyOverviewView = [];
        let doctorPatientsView = [];
        let doctorScheduleView = [];
        let patientAppointmentsView = [];
        let hospitalAdminsView = [];
        let deptCache = [];
        // 登录令牌：仅存内存，刷新页面即失效，需要重新登录
        let authToken = null;
        
        /* ---------- UI 基础设施：HTML 转义 / Toast / 顶部加载条 ---------- */
        function esc(value) {
            return String(value == null ? '' : value)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        }

        /* ---------- 医生头像：AI 生成职业照，同一医生固定同一张 ---------- */
        const MALE_DOCTOR_AVATARS = [
            'professional headshot portrait of a Chinese male doctor in his 30s, short black hair, white coat, stethoscope, light hospital background, friendly smile, realistic photography, square crop',
            'professional headshot portrait of a Chinese male doctor in his 40s, glasses, white coat, clinic background, confident expression, realistic photography, square crop',
            'professional headshot portrait of a Chinese male doctor in his 20s, white coat, modern hospital corridor background, friendly, realistic photography, square crop',
            'professional headshot portrait of a Chinese male doctor in his 50s, gray hair, white coat, experienced, hospital background, realistic photography, square crop',
            'professional headshot portrait of a Chinese male doctor in his 40s, clean shaven, white coat, blue clinic background, professional, realistic photography, square crop'
        ];
        const FEMALE_DOCTOR_AVATARS = [
            'professional headshot portrait of a Chinese female doctor in her 30s, long black hair, white coat, bright hospital background, warm smile, realistic photography, square crop',
            'professional headshot portrait of a Chinese female doctor in her 40s, shoulder length hair, white coat, stethoscope, hospital background, realistic photography, square crop',
            'professional headshot portrait of a Chinese female doctor in her 50s, short hair, glasses, white coat, calm expression, realistic photography, square crop',
            'professional headshot portrait of a Chinese female doctor in her 20s, ponytail, white coat, modern hospital background, bright smile, realistic photography, square crop',
            'professional headshot portrait of a Chinese female doctor in her 30s, glasses, white coat, stethoscope, hospital background, professional, realistic photography, square crop'
        ];
        function doctorAvatarUrl(doctor) {
            const seed = String((doctor && (doctor.doctorId || doctor.id || doctor.name)) || 'doctor');
            let hash = 0;
            for (let i = 0; i < seed.length; i++) {
                hash = (hash * 31 + seed.charCodeAt(i)) >>> 0;
            }
            const gender = doctor && doctor.gender;
            const isFemale = gender === 'F' || gender === '女';
            const isMale = gender === 'M' || gender === '男';
            const pool = isFemale ? FEMALE_DOCTOR_AVATARS : (isMale ? MALE_DOCTOR_AVATARS : MALE_DOCTOR_AVATARS.concat(FEMALE_DOCTOR_AVATARS));
            const prompt = pool[hash % pool.length];
            return 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt='
                + encodeURIComponent(prompt) + '&image_size=square';
        }
        function doctorAvatarHtml(doctor, name, sizeClass) {
            const initial = (name || '医').trim().charAt(0);
            return `<span class="doctor-avatar-box ${sizeClass || 'avatar-sm'}">
                <img src="${doctorAvatarUrl(doctor)}" alt="${esc(name || '医生')}头像" loading="lazy"
                     onerror="this.style.display='none';this.nextElementSibling.style.display='flex';">
                <span class="doctor-avatar-fallback">${esc(initial)}</span>
            </span>`;
        }
        
        let toastTimer = null;
        function showToast(message, type = 'info') {
            const el = document.getElementById('app-toast');
            if (!el) return;
            el.textContent = message;
            el.className = 'app-toast show ' + type;
            clearTimeout(toastTimer);
            toastTimer = setTimeout(() => { el.className = 'app-toast ' + type; }, 2600);
        }
        
        /* 原生 alert 会阻塞页面，统一替换为内联 toast */
        window.alert = function (message) { showToast(message, 'info'); };

        /* 按钮点击波纹反馈 */
        document.addEventListener('click', function (event) {
            const target = event.target.closest('button, .btn');
            if (!target || target.disabled || target.classList.contains('disabled')) return;
            const rect = target.getBoundingClientRect();
            const size = Math.max(rect.width, rect.height);
            const ripple = document.createElement('span');
            ripple.className = 'btn-ripple';
            ripple.style.width = size + 'px';
            ripple.style.height = size + 'px';
            ripple.style.left = (event.clientX - rect.left - size / 2) + 'px';
            ripple.style.top = (event.clientY - rect.top - size / 2) + 'px';
            target.appendChild(ripple);
            setTimeout(() => ripple.remove(), 600);
        });
        
        let pendingRequests = 0;
        function setProgress(delta) {
            const bar = document.getElementById('top-progress');
            pendingRequests = Math.max(0, pendingRequests + delta);
            if (bar) bar.classList.toggle('active', pendingRequests > 0);
        }
        
        async function fetchData(url, method = 'GET', body = null) {
            setProgress(1);
            try {
                const options = { method };
                const headers = {};
                if (body) {
                    headers['Content-Type'] = 'application/json';
                    options.body = JSON.stringify(body);
                }
                if (authToken) {
                    headers['X-Auth-Token'] = authToken;
                }
                options.headers = headers;
                const response = await fetch(API_BASE + url, options);
                if (response.status === 401) {
                    authToken = null;
                    showToast('登录已失效，请重新登录', 'error');
                    logout();
                    return [];
                }
                if (!response.ok) {
                    let message = '请求失败（' + response.status + '）';
                    try {
                        const payload = await response.json();
                        if (payload && payload.error) message = payload.error;
                    } catch (ignored) { /* 响应体不是 JSON 时沿用默认提示 */ }
                    showToast(message, 'error');
                    return [];
                }
                if (response.status === 204) return null;
                const text = await response.text();
                return text ? JSON.parse(text) : null;
            } catch (error) {
                console.error('API Error:', error);
                showToast('网络连接异常，请稍后重试', 'error');
                return [];
            } finally {
                setProgress(-1);
            }
        }
        
        function selectRole(role) {
            currentRole = role;
            document.querySelectorAll('.role-btn').forEach(r => r.classList.remove('active'));
            document.getElementById('role-' + role).classList.add('active');
            
            if (role === 'patient') {
                document.getElementById('label-username').textContent = '手机号';
                document.getElementById('login-hint').textContent = '患者账号需要先注册';
                document.getElementById('register-section').style.display = 'block';
                document.getElementById('login-username').value = '';
                document.getElementById('login-password').value = '';
            } else if (role === 'doctor') {
                document.getElementById('label-username').textContent = '用户名';
                document.getElementById('login-hint').textContent = '医生账号: wangjianan / 123456';
                document.getElementById('register-section').style.display = 'block';
                document.getElementById('login-username').value = 'wangjianan';
                document.getElementById('login-password').value = '123456';
            } else {
                document.getElementById('label-username').textContent = '用户名';
                document.getElementById('login-hint').textContent = '管理员账号: admin / admin123';
                document.getElementById('register-section').style.display = 'none';
                document.getElementById('login-username').value = 'admin';
                document.getElementById('login-password').value = 'admin123';
            }
            updateLoginModeUI();
        }

        /* ---------- 患者端：密码 / 验证码 双模式登录 ---------- */
        let loginMode = 'password';
        let codeTicker = null;

        function updateLoginModeUI() {
            const isPatient = currentRole === 'patient';
            if (!isPatient) {
                loginMode = 'password';
                document.getElementById('login-code').value = '';
            }
            document.getElementById('mode-switch').style.display = isPatient ? 'block' : 'none';
            document.getElementById('login-password-group').style.display = (isPatient && loginMode === 'code') ? 'none' : 'block';
            document.getElementById('login-code-group').style.display = (isPatient && loginMode === 'code') ? 'block' : 'none';
            document.getElementById('mode-link').textContent = loginMode === 'code' ? '使用密码登录' : '使用验证码登录';
            document.getElementById('login-username').placeholder = isPatient ? '请输入手机号' : '请输入用户名';
        }

        function toggleLoginMode() {
            loginMode = loginMode === 'code' ? 'password' : 'code';
            updateLoginModeUI();
        }

        async function sendLoginCode() {
            const phone = document.getElementById('login-username').value.trim();
            if (!/^1[3-9]\d{9}$/.test(phone)) {
                showToast('请输入正确的手机号', 'error');
                return;
            }

            const btn = document.getElementById('send-code-btn');
            btn.disabled = true;

            const result = await fetchData('/patients/send-code', 'POST', { phone });
            if (!result || !result.success) {
                btn.disabled = false;
                showToast((result && result.message) || '验证码发送失败', 'error');
                return;
            }

            showToast(result.devCode ? ('开发模式验证码：' + result.devCode) : '验证码已发送');
            startCodeCountdown(btn);
        }

        function startCodeCountdown(btn) {
            let seconds = 60;
            btn.textContent = seconds + 's';
            clearInterval(codeTicker);
            codeTicker = setInterval(function () {
                seconds -= 1;
                if (seconds <= 0) {
                    clearInterval(codeTicker);
                    btn.disabled = false;
                    btn.textContent = '获取验证码';
                } else {
                    btn.textContent = seconds + 's';
                }
            }, 1000);
        }

        async function login() {
            const username = document.getElementById('login-username').value.trim();
            let result;

            const loginBtn = document.getElementById('login-btn');
            if (loginBtn && loginBtn.disabled) return;
            const loginBtnHtml = loginBtn ? loginBtn.innerHTML : '';
            if (loginBtn) {
                loginBtn.disabled = true;
                loginBtn.classList.add('is-loading');
                loginBtn.innerHTML = '<span class="spinner"></span>登录中';
            }

            try {
            if (currentRole === 'patient' && loginMode === 'code') {
                const code = document.getElementById('login-code').value.trim();
                if (!username || !code) {
                    showToast('请输入手机号和验证码', 'error');
                    return;
                }
                result = await fetchData('/patients/login-by-code', 'POST', { phone: username, code });
            } else {
                const password = document.getElementById('login-password').value;
                if (!username || !password) {
                    showToast('请输入用户名和密码', 'error');
                    return;
                }
                if (currentRole === 'patient') {
                    result = await fetchData('/patients/login', 'POST', { phone: username, password });
                } else if (currentRole === 'doctor') {
                    result = await fetchData('/doctor/login', 'POST', { username, password });
                } else {
                    result = await fetchData('/admin/login', 'POST', { username, password });
                }
            }
            
            console.log('Login result:', result);
            
            if (result && (result.success === true || result.username || result.name)) {
                authToken = result.token || null;
                currentUser = result.username || result.name || result.patientName || username;
                if (currentRole === 'patient') {
                    currentUserId = result.patientId || result.id || result.patient?.id;
                } else if (currentRole === 'doctor') {
                    currentUserId = result.doctorId || result.id || result.doctor?.doctorId;
                } else {
                    currentUserId = result.adminId || result.id || result.admin?.adminId;
                    currentAdminRole = result.role || 'HOSPITAL_ADMIN';
                    currentHospitalId = result.hospitalId || null;
                }
                
                document.getElementById('current-user').textContent = currentUser;
                document.getElementById('current-role').textContent = getRoleName(currentRole, currentAdminRole);
                document.getElementById('login-container').style.display = 'none';
                document.getElementById('main-container').style.display = 'block';
                
                setupTabs();
                if (currentRole === 'admin') {
                    showTab(currentAdminRole === 'SUPER_ADMIN' ? 'admin-hospitals' : 'admin-depts');
                } else {
                    showTab('hospitals');
                }
            } else {
                const message = (result && result.message) || '登录失败，请检查账号或密码';
                showToast(message, 'error');
                const errorBox = document.getElementById('login-error');
                errorBox.textContent = message;
                errorBox.style.display = 'block';
                setTimeout(() => {
                    errorBox.style.display = 'none';
                }, 3000);
            }
            } finally {
                if (loginBtn) {
                    loginBtn.disabled = false;
                    loginBtn.classList.remove('is-loading');
                    loginBtn.innerHTML = loginBtnHtml;
                }
            }
        }
        
        function getRoleName(role, adminRole) {
            if (role === 'admin') {
                return adminRole === 'SUPER_ADMIN' ? '卫健委总管理员' : '医院管理员';
            }
            const names = { doctor: '医生', patient: '患者' };
            return names[role] || '用户';
        }
        
        function logout() {
            authToken = null;
            currentUser = null;
            currentRole = 'admin';
            currentUserId = null;
            currentAdminRole = null;
            currentHospitalId = null;
            adminHospitalPage = 0;
            adminAdminPage = 0;
            loginMode = 'password';
            clearInterval(codeTicker);
            document.getElementById('login-code').value = '';
            document.getElementById('send-code-btn').disabled = false;
            document.getElementById('send-code-btn').textContent = '获取验证码';
            document.getElementById('main-container').style.display = 'none';
            document.getElementById('login-container').style.display = 'flex';
            document.getElementById('login-error').style.display = 'none';
            document.getElementById('role-admin').click();
        }
        
        function selectRegRole(role) {
            currentRegRole = role;
            document.querySelectorAll('#register-modal .role-btn').forEach(r => r.classList.remove('active'));
            document.getElementById('reg-role-' + role).classList.add('active');
            
            document.getElementById('register-form-patient').style.display = 'none';
            document.getElementById('register-form-doctor').style.display = 'none';
            document.getElementById('register-form-admin').style.display = 'none';
            
            const titles = { patient: '患者注册', doctor: '医生注册', admin: '管理员注册' };
            document.getElementById('register-title').textContent = titles[role];
            document.getElementById('register-form-' + role).style.display = 'block';
        }
        
        function setupTabs() {
            const tabs = document.getElementById('main-tabs');
            
            if (currentRole === 'admin') {
                if (currentAdminRole === 'SUPER_ADMIN') {
                    tabs.innerHTML = `
                        <button class="tab active" onclick="showTab('admin-hospitals')">医院准入</button>
                        <button class="tab" onclick="showTab('admin-admins')">管理员审批</button>
                    `;
                } else {
                    tabs.innerHTML = `
                        <button class="tab active" onclick="showTab('admin-depts')">科室管理</button>
                        <button class="tab" onclick="showTab('admin-doctors')">医生管理</button>
                        <button class="tab" onclick="showTab('duty-rules')">出诊规则</button>
                        <button class="tab" onclick="showTab('admin-schedule')">排班管理</button>
                        <button class="tab" onclick="showTab('duty-overview')">在岗达标</button>
                        <button class="tab" onclick="showTab('admin-applications')">管理员申请</button>
                    `;
                }
            } else if (currentRole === 'doctor') {
                tabs.innerHTML = `
                    <button class="tab active" onclick="showTab('hospitals')">医院信息</button>
                    <button class="tab" onclick="showTab('doctor-schedule')">我的排班</button>
                    <button class="tab" onclick="showTab('doctor-patients')">我的患者</button>
                `;
            } else {
                tabs.innerHTML = `
                    <button class="tab active" onclick="showTab('hospitals')">医院列表</button>
                    <button class="tab" onclick="showTab('doctors')">医生列表</button>
                    <button class="tab" onclick="showTab('patient-profile')">个人中心</button>
                    <button class="tab" onclick="showTab('schedule')">预约挂号</button>
                `;
            }
        }
        
        function showTab(tabName) {
            document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
            const activeTab = document.querySelector(`.tab[onclick="showTab('${tabName}')"]`);
            if (activeTab) activeTab.classList.add('active');
            
            document.querySelectorAll('[id$="-tab"]').forEach(t => t.style.display = 'none');
            const panel = document.getElementById(`${tabName}-tab`);
            panel.style.display = 'block';
            panel.classList.remove('panel-enter');
            void panel.offsetWidth;
            panel.classList.add('panel-enter');
            
            if (tabName === 'hospitals') loadHospitals();
            else if (tabName === 'doctors') loadDoctors();
            else if (tabName === 'appointments') loadAppointments();
            else if (tabName === 'schedule') loadScheduleData();
            else if (tabName === 'doctor-schedule') loadDoctorScheduleList();
            else if (tabName === 'doctor-patients') loadDoctorPatients();
            else if (tabName === 'patient-profile') loadPatientProfile();
            else if (tabName === 'admin-hospitals') loadAdminHospitals(true);
            else if (tabName === 'admin-admins') loadAdminAdmins(true);
            else if (tabName === 'admin-depts') loadAdminDepartments();
            else if (tabName === 'admin-doctors') loadAdminDoctors();
            else if (tabName === 'duty-rules') loadDutyRulePage();
            else if (tabName === 'admin-schedule') loadAdminSchedule(currentScheduleDoctorId);
            else if (tabName === 'duty-overview') loadDutyOverview(true);
            else if (tabName === 'admin-applications') loadHospitalAdmins();
        }
        
        async function loadHospitals() {
            hospitals = await fetchData('/hospitals');
            listPages.hospitals = 0;
            renderHospitals(hospitals);
        }
        
        function renderHospitals(data) {
            const container = document.getElementById('hospital-list');
            hospitalsView = data;
            if (data.length === 0) {
                container.innerHTML = '<p class="empty-tip">暂无医院数据</p>';
                renderPager('hospital-pager', 0, 0, null, null, 0);
                return;
            }
            const { slice, page, totalPages, total } = pageSlice(data, 'hospitals');
            container.innerHTML = slice.map(h => {
                const name = h.hospitalName || h.name || '未命名医院';
                const addr = h.address || '地址未填写';
                const phone = h.phone || '电话未填写';
                const level = h.level || '等级未填写';
                return `
                    <div class="card">
                        <h3>${esc(name)}</h3>
                        <p><strong>地址:</strong> ${esc(addr)}</p>
                        <p><strong>等级:</strong> ${esc(level)}</p>
                        <p><strong>电话:</strong> ${esc(phone)}</p>
                        <div class="card-actions">
                            <a href="#" class="btn" onclick="viewHospitalDoctors('${h.hospitalId || h.id || ''}'); return false;">查看医生</a>
                        </div>
                    </div>
                `;
            }).join('');
            renderPager('hospital-pager', page, totalPages,
                () => gotoPage('hospitals', page - 1, () => renderHospitals(hospitalsView)),
                () => gotoPage('hospitals', page + 1, () => renderHospitals(hospitalsView)),
                total);
        }
        
        function searchHospitals() {
            const keyword = document.getElementById('search-hospital').value.toLowerCase();
            const filtered = hospitals.filter(h => 
                (h.hospitalName || '').toLowerCase().includes(keyword) ||
                (h.address || '').toLowerCase().includes(keyword)
            );
            listPages.hospitals = 0;
            renderHospitals(filtered);
        }
        
        async function loadDoctors() {
            doctors = await fetchData('/doctors');
            listPages.doctors = 0;
            renderDoctors(doctors);
            populateHospitalSelect();
        }
        
        function populateHospitalSelect() {
            const select = document.getElementById('filter-hospital');
            const hospitalSet = new Set(doctors.map(d => d.hospitalId).filter(Boolean));
            const hospitalNames = {};
            hospitals.forEach(h => {
                hospitalNames[h.hospitalId || h.id] = h.hospitalName || h.name;
            });
            select.innerHTML = '<option value="">全部医院</option>' + 
                Array.from(hospitalSet).map(hid => `<option value="${hid}">${hospitalNames[hid] || hid}</option>`).join('');
        }
        
        function renderDoctors(data) {
            const container = document.getElementById('doctor-list');
            doctorsView = data;
            if (data.length === 0) {
                container.innerHTML = '<p class="empty-tip">暂无医生数据</p>';
                renderPager('doctor-pager', 0, 0, null, null, 0);
                return;
            }
            const { slice, page, totalPages, total } = pageSlice(data, 'doctors');
            container.innerHTML = slice.map(d => {
                const doctorName = d.doctorName || d.name || '未知医生';
                const hospitalName = hospitals.find(h => h.hospitalId === d.hospitalId || h.id === d.hospitalId);
                return `
                    <div class="card">
                        <div class="doctor-head">
                            ${doctorAvatarHtml(d, doctorName)}
                            <div class="doctor-head-info">
                                <h3>${esc(doctorName)}</h3>
                                <p class="muted">${esc(d.title || '医师')} · ${esc(d.department || d.deptId || '未填写')}</p>
                            </div>
                        </div>
                        <p><strong>医院:</strong> ${esc(hospitalName ? (hospitalName.hospitalName || hospitalName.name) : d.hospitalId)}</p>
                        <p><strong>简介:</strong> ${esc((d.introduction || d.expertise || '未填写').substring(0, 50))}...</p>
                        <div class="card-actions">
                            <a href="#" class="btn" onclick="viewDoctor('${d.doctorId || d.id || ''}'); return false;">查看详情</a>
                        </div>
                    </div>
                `;
            }).join('');
            renderPager('doctor-pager', page, totalPages,
                () => gotoPage('doctors', page - 1, () => renderDoctors(doctorsView)),
                () => gotoPage('doctors', page + 1, () => renderDoctors(doctorsView)),
                total);
        }
        
        function searchDoctors() {
            const hospital = document.getElementById('filter-hospital').value;
            const keyword = document.getElementById('search-doctor').value.toLowerCase();
            let filtered = doctors;
            if (hospital) filtered = filtered.filter(d => d.hospitalId === hospital);
            if (keyword) filtered = filtered.filter(d => 
                (d.doctorName || d.name || '').toLowerCase().includes(keyword)
            );
            listPages.doctors = 0;
            renderDoctors(filtered);
        }
        
        async function viewDoctor(doctorId) {
            if (!doctorId) {
                alert('无法查看详情，医生ID为空');
                return;
            }
            const doctor = await fetchData(`/doctors/${doctorId}`);
            if (!doctor) {
                alert('未找到医生信息');
                return;
            }
            
            const name = doctor.doctorName || doctor.name || '未知';
            const hospitalName = hospitals.find(h => h.hospitalId === doctor.hospitalId || h.id === doctor.hospitalId);
            document.getElementById('doctor-detail').innerHTML = `
                <div class="doctor-profile">
                    ${doctorAvatarHtml(doctor, name, 'avatar-lg')}
                    <div class="doctor-info">
                        <h3>${esc(name)}</h3>
                        <p><strong>职称:</strong> ${esc(doctor.title || '未填写')}</p>
                        <p><strong>医院:</strong> ${esc(hospitalName ? (hospitalName.hospitalName || hospitalName.name) : doctor.hospitalId || '未填写')}</p>
                        <p><strong>科室:</strong> ${esc(doctor.department || doctor.deptId || '未填写')}</p>
                    </div>
                </div>
                <div style="margin-top: 20px;">
                    <p><strong>专业擅长:</strong> ${esc(doctor.specialty || doctor.expertise || '未填写')}</p>
                    <p><strong>个人简介:</strong> ${esc(doctor.introduction || '未填写')}</p>
                    <p><strong>性别:</strong> ${doctor.gender === 'M' || doctor.gender === '男' ? '男' : '女'}</p>
                    <p><strong>年龄:</strong> ${esc(doctor.age || '未填写')}</p>
                    <p><strong>挂号费:</strong> ¥${esc(doctor.consultationFee || 0)}</p>
                </div>
            `;
            document.getElementById('doctor-modal').classList.add('show');
        }
        
        function closeModal() {
            document.getElementById('doctor-modal').classList.remove('show');
        }
        
        function viewHospitalDoctors(hospitalId) {
            document.getElementById('filter-hospital').value = hospitalId;
            showTab('doctors');
            setTimeout(() => searchDoctors(), 100);
        }
        
        async function loadAppointments() {
            const data = await fetchData('/appointments');
            listPages.appointments = 0;
            renderAppointments(data);
            loadStats();
        }
        
        function getStatusBadge(status) {
            const statusMap = { 
                0: '已取消', 
                1: '待确认', 
                2: '已确认',
                3: '就诊中',
                'canceled': '已取消',
                'pending': '待确认',
                'confirmed': '已确认',
                'completed': '已完成'
            };
            const classMap = { 
                0: 'status-canceled', 
                1: 'status-pending', 
                2: 'status-confirmed',
                3: 'status-completed',
                'canceled': 'status-canceled',
                'pending': 'status-pending',
                'confirmed': 'status-confirmed',
                'completed': 'status-completed'
            };
            const statusNum = typeof status === 'string' ? status : parseInt(status);
            return `<span class="status-badge ${classMap[statusNum] || 'status-pending'}">${statusMap[statusNum] || '未知'}</span>`;
        }
        
        function renderAppointments(data) {
            const container = document.getElementById('appointment-list');
            appointmentsView = data;
            if (data.length === 0) {
                container.innerHTML = '<p class="empty-tip">暂无预约数据</p>';
                renderPager('appointment-pager', 0, 0, null, null, 0);
                return;
            }
            const { slice, page, totalPages, total } = pageSlice(data, 'appointments');
            container.innerHTML = slice.map(a => `
                <div class="card">
                    <h3>预约 ${esc(a.appointmentId || a.id || '未知')}</h3>
                    <p><strong>医生:</strong> ${esc(a.doctorId || '未填写')}</p>
                    <p><strong>患者:</strong> ${esc(a.patientId || '未填写')}</p>
                    <p><strong>日期:</strong> ${esc(a.appointmentDate || a.date || '未填写')}</p>
                    <p><strong>时段:</strong> ${esc(a.timeSlot || '未填写')}</p>
                    <p><strong>状态:</strong> ${getStatusBadge(a.status)}</p>
                    ${(a.status === 1 || a.status === '1' || a.status === 'pending') ? `<div class="card-actions"><a href="#" class="btn btn-danger" onclick="cancelAppointment('${a.appointmentId || a.id || ''}'); return false;">取消预约</a></div>` : ''}
                </div>
            `).join('');
            renderPager('appointment-pager', page, totalPages,
                () => gotoPage('appointments', page - 1, () => renderAppointments(appointmentsView)),
                () => gotoPage('appointments', page + 1, () => renderAppointments(appointmentsView)),
                total);
        }
        
        function searchAppointments() {
            const keyword = document.getElementById('search-appointment').value.toLowerCase();
            fetchData('/appointments').then(data => {
                const filtered = data.filter(a => 
                    (a.appointmentId || '').toLowerCase().includes(keyword) ||
                    (a.patientId || '').toLowerCase().includes(keyword)
                );
                listPages.appointments = 0;
                renderAppointments(filtered);
            });
        }
        
        async function cancelAppointment(id) {
            if (!id) {
                alert('无法取消，预约ID为空');
                return;
            }
            if (!confirm('确定要取消这个预约吗？')) return;
            await fetch(API_BASE + `/appointments/${id}`, { method: 'DELETE' });
            loadAppointments();
        }
        
        async function loadScheduleData() {
            hospitals = await fetchData('/hospitals');
            const select = document.getElementById('schedule-hospital');
            select.innerHTML = '<option value="">请选择医院</option>' + 
                hospitals.map(h => {
                    const name = h.hospitalName || h.name || '未命名医院';
                    return `<option value="${h.hospitalId || h.id || ''}">${name}</option>`;
                }).join('');
            
            const today = new Date().toISOString().split('T')[0];
            document.getElementById('schedule-date').value = today;
        }
        
        async function loadHospitalDoctors() {
            const hospitalId = document.getElementById('schedule-hospital').value;
            const select = document.getElementById('schedule-doctor');
            const slotSelect = document.getElementById('schedule-slot');
            
            if (!hospitalId) {
                select.innerHTML = '<option value="">请选择医生</option>';
                slotSelect.innerHTML = '<option value="">请选择排班</option>';
                return;
            }
            const data = await fetchData(`/doctors/hospital/${hospitalId}`);
            
            if (!data || data.length === 0) {
                select.innerHTML = '<option value="">该医院暂无医生</option>';
                slotSelect.innerHTML = '<option value="">请选择排班</option>';
                return;
            }
            
            select.innerHTML = '<option value="">请选择医生</option>' + 
                data.map(d => {
                    const name = d.doctorName || d.name || '未知医生';
                    return `<option value="${d.doctorId || d.id || ''}">${name} - ${d.title || ''}</option>`;
                }).join('');
        }
        
        async function loadDoctorSchedule() {
            const doctorId = document.getElementById('schedule-doctor').value;
            const select = document.getElementById('schedule-slot');
            console.log('loadDoctorSchedule called, doctorId:', doctorId);
            
            if (!doctorId) {
                select.innerHTML = '<option value="">请选择排班</option>';
                return;
            }
            
            const data = await fetchData(`/schedules/doctor/${doctorId}`);
            console.log('Schedule data received:', data);
            
            if (!data || data.length === 0) {
                select.innerHTML = '<option value="">该医生暂无排班</option>';
            } else {
                select.innerHTML = '<option value="">请选择排班</option>' + 
                    data.map(s => `<option value="${esc(s.scheduleId || s.id || '')}">${esc(s.workDate || s.date || s.appointmentDate || '')} ${esc(s.timeSlot || '')}</option>`).join('');
            }
        }
        
        async function createAppointment() {
            const doctorId = document.getElementById('schedule-doctor').value;
            const scheduleId = document.getElementById('schedule-slot').value;
            const date = document.getElementById('schedule-date').value;
            const patientId = document.getElementById('schedule-patient').value;
            
            if (!doctorId || !scheduleId || !date) {
                alert('请填写所有必填项！');
                return;
            }
            
            if (currentRole === 'patient' && !patientId) {
                alert('请输入患者ID！');
                return;
            }
            
            const data = {
                doctorId,
                patientId: patientId || currentUserId,
                scheduleId,
                appointmentDate: date,
                timeSlot: document.getElementById('schedule-slot').options[document.getElementById('schedule-slot').selectedIndex].text.split(' ').pop() || ''
            };
            
            const result = await fetchData('/appointments', 'POST', data);
            
            if (result && result.success) {
                showToast('预约成功', 'success');
                document.getElementById('schedule-patient').value = '';
                showTab(currentRole === 'patient' ? 'patient-profile' : 'appointments');
            } else {
                showToast((result && result.message) || '预约失败，请重试', 'error');
            }
        }
        
        async function loadDoctorScheduleList() {
            doctorScheduleView = (await fetchData(`/schedules/doctor/${currentUserId}`)) || [];
            listPages.doctorSchedules = 0;
            renderDoctorSchedulesPage();
        }
        function renderDoctorSchedulesPage() {
            const container = document.getElementById('doctor-schedule-list');
            if (!doctorScheduleView.length) {
                container.innerHTML = '<p class="empty-tip">暂无排班数据</p>';
                renderPager('doctor-schedule-pager', 0, 0, null, null, 0);
                return;
            }
            const { slice, page, totalPages, total } = pageSlice(doctorScheduleView, 'doctorSchedules');
            container.innerHTML = slice.map(s => `
                <div class="card">
                    <h3>${esc(s.workDate || '未填写')}</h3>
                    <p><strong>时段:</strong> ${esc(s.timeSlot || '未填写')}</p>
                    <p><strong>最大预约:</strong> ${s.maxPatients || 20} 人</p>
                    <p><strong>已预约:</strong> ${s.bookedCount || 0} 人</p>
                    <p><strong>剩余:</strong> ${(s.maxPatients - (s.bookedCount || 0)) || 0} 人</p>
                    <p><strong>状态:</strong> ${s.status === 1 ? '<span class="status-badge status-confirmed">可预约</span>' : '<span class="status-badge status-canceled">已关闭</span>'}</p>
                    <div class="card-actions">
                        <a href="#" class="btn btn-danger" onclick="deleteSchedule('${s.scheduleId || s.id || ''}'); return false;">删除排班</a>
                    </div>
                </div>
            `).join('');
            renderPager('doctor-schedule-pager', page, totalPages,
                () => gotoPage('doctorSchedules', page - 1, () => renderDoctorSchedulesPage()),
                () => gotoPage('doctorSchedules', page + 1, () => renderDoctorSchedulesPage()),
                total);
        }
        
        async function deleteSchedule(scheduleId) {
            if (!scheduleId) {
                alert('无法删除，排班ID为空');
                return;
            }
            if (!confirm('确定要删除这个排班吗？')) return;
            
            const result = await fetchData(`/schedules/${scheduleId}`, 'DELETE');
            if (result && result.success) {
                alert('排班删除成功！');
                loadDoctorScheduleList();
            } else {
                alert(result ? result.message : '删除失败');
            }
        }
        
        async function loadDoctorPatients() {
            const data = await fetchData(`/doctor/${currentUserId}/patients`);
            const container = document.getElementById('doctor-patients-list');
            doctorPatientsView = (data && data.patients) ? data.patients : [];
            listPages.doctorPatients = 0;
            renderDoctorPatients();
        }
        function renderDoctorPatients() {
            const container = document.getElementById('doctor-patients-list');
            if (!doctorPatientsView.length) {
                container.innerHTML = '<p class="empty-tip">暂无患者数据</p>';
                renderPager('doctor-patient-pager', 0, 0, null, null, 0);
                return;
            }
            const { slice, page, totalPages, total } = pageSlice(doctorPatientsView, 'doctorPatients');
            container.innerHTML = slice.map(p => `
                <div class="card">
                    <h3>${esc(p.name || '未知患者')}</h3>
                    <p><strong>患者ID:</strong> ${esc(p.patientId)}</p>
                    <p><strong>性别:</strong> ${p.gender === 'M' ? '男' : '女'}</p>
                    <p><strong>年龄:</strong> ${p.age || '未填写'}</p>
                    <p><strong>电话:</strong> ${esc(p.phone || '未填写')}</p>
                    ${p.medicalHistory ? `<p><strong>病史:</strong> ${esc(p.medicalHistory)}</p>` : ''}
                </div>
            `).join('');
            renderPager('doctor-patient-pager', page, totalPages,
                () => gotoPage('doctorPatients', page - 1, () => renderDoctorPatients()),
                () => gotoPage('doctorPatients', page + 1, () => renderDoctorPatients()),
                total);
        }
        
        async function loadPatientProfile() {
            const data = await fetchData(`/patients/${currentUserId}`);
            const profileContainer = document.getElementById('patient-profile');
            
            if (!data) {
                profileContainer.innerHTML = '<p style="text-align: center; color: #999;">无法获取个人信息</p>';
                return;
            }
            
            profileContainer.innerHTML = `
                <div class="info-item"><div class="label">患者ID</div><div class="value">${data.patientId || '未填写'}</div></div>
                <div class="info-item"><div class="label">姓名</div><div class="value">${esc(data.name || '未填写')}</div></div>
                <div class="info-item"><div class="label">性别</div><div class="value">${data.gender === 'M' ? '男' : '女'}</div></div>
                <div class="info-item"><div class="label">年龄</div><div class="value">${data.age || '未填写'}</div></div>
                <div class="info-item"><div class="label">电话</div><div class="value">${esc(data.phone || '未填写')}</div></div>
                <div class="info-item"><div class="label">身份证号</div><div class="value">${esc(data.idCard || '未填写')}</div></div>
            `;
            
            const appData = await fetchData('/appointments');
            patientAppointmentsView = appData.filter(a => a.patientId === currentUserId);
            listPages.patientAppointments = 0;
            renderPatientAppointments();
        }
        function renderPatientAppointments() {
            const appContainer = document.getElementById('patient-appointments');
            if (!patientAppointmentsView.length) {
                appContainer.innerHTML = '<p class="empty-tip">暂无预约记录</p>';
                renderPager('patient-appointment-pager', 0, 0, null, null, 0);
                return;
            }
            const { slice, page, totalPages, total } = pageSlice(patientAppointmentsView, 'patientAppointments');
            appContainer.innerHTML = slice.map(a => {
                return `
                    <div class="card">
                        <h3>预约 ${esc(a.appointmentId || a.id || '未预约')}</h3>
                        <p><strong>医院:</strong> ${esc(a.hospitalName || '未知医院')}</p>
                        <p><strong>医生:</strong> ${esc(a.doctorName || a.doctorId)}</p>
                        <p><strong>日期:</strong> ${esc(a.appointmentDate || '未填写')}</p>
                        <p><strong>时段:</strong> ${esc(a.timeSlot || '未填写')}</p>
                        <p><strong>状态:</strong> ${getStatusBadge(a.status)}</p>
                        ${(a.status === 1 || a.status === '1' || a.status === 'pending') ? `<div class="card-actions"><a href="#" class="btn btn-danger" onclick="cancelAppointment('${a.appointmentId || a.id || ''}'); return false;">取消预约</a></div>` : ''}
                    </div>
                `;
            }).join('');
            renderPager('patient-appointment-pager', page, totalPages,
                () => gotoPage('patientAppointments', page - 1, () => renderPatientAppointments()),
                () => gotoPage('patientAppointments', page + 1, () => renderPatientAppointments()),
                total);
        }
        
        async function loadStats() {
            const hospitalsData = await fetchData('/hospitals');
            const doctorsData = await fetchData('/doctors');
            const appointmentsData = await fetchData('/appointments');
            
            document.getElementById('stat-hospitals').textContent = hospitalsData.length;
            document.getElementById('stat-doctors').textContent = doctorsData.length;
            document.getElementById('stat-appointments').textContent = appointmentsData.length;
            document.getElementById('stat-pending').textContent = appointmentsData.filter(a => a.status === 1 || a.status === '1' || a.status === 'pending').length;
            document.getElementById('stats').style.display = 'grid';
        }
        
        function showRegister() {
            currentRegRole = currentRole;
            selectRegRole(currentRole);
            document.getElementById('register-modal').classList.add('show');
        }
        
        function closeRegister() {
            document.getElementById('register-modal').classList.remove('show');
        }
        
        async function register() {
            if (currentRegRole === 'patient') {
                await registerPatient();
            } else if (currentRegRole === 'doctor') {
                await registerDoctor();
            } else {
                await registerAdmin();
            }
        }
        
        async function registerPatient() {
            const name = document.getElementById('reg-name').value;
            const phone = document.getElementById('reg-phone').value;
            const password = document.getElementById('reg-password').value;
            const gender = document.getElementById('reg-gender').value;
            const age = document.getElementById('reg-age').value;
            const idCard = document.getElementById('reg-idcard').value;
            const medicalHistory = document.getElementById('reg-history').value;
            
            if (!name || !phone || !password || !gender) {
                alert('请填写必填项（姓名、手机号、密码、性别）');
                return;
            }
            
            const result = await fetchData('/patients/register', 'POST', {
                name, phone, password, gender, age, idCard, medicalHistory
            });
            
            if (result && (result.success === true || result.patientId || result.id)) {
                closeRegister();
                document.getElementById('register-success').style.display = 'block';
                document.getElementById('login-username').value = phone;
                document.getElementById('login-password').value = password;
                setTimeout(() => {
                    document.getElementById('register-success').style.display = 'none';
                }, 3000);
            } else {
                alert(result ? result.message : '注册失败');
            }
        }
        
        async function registerDoctor() {
            const username = document.getElementById('reg-username').value;
            const password = document.getElementById('reg-password-doc').value;
            const name = document.getElementById('reg-name-doc').value;
            const hospitalId = document.getElementById('reg-hospital-id').value;
            const title = document.getElementById('reg-title').value;
            const dept = document.getElementById('reg-dept').value;
            
            if (!username || !password || !name) {
                alert('请填写必填项（用户名、密码、姓名）');
                return;
            }
            
            const result = await fetchData('/doctor/register', 'POST', {
                username, password, name, hospitalId: hospitalId || 'H001', title, deptId: dept
            });
            
            if (result && (result.success === true || result.doctorId || result.id)) {
                closeRegister();
                document.getElementById('register-success').style.display = 'block';
                document.getElementById('login-username').value = username;
                document.getElementById('login-password').value = password;
                setTimeout(() => {
                    document.getElementById('register-success').style.display = 'none';
                }, 3000);
            } else {
                alert(result ? result.message : '注册失败');
            }
        }
        
        async function registerAdmin() {
            const username = document.getElementById('reg-admin-username').value;
            const password = document.getElementById('reg-admin-password').value;
            const name = document.getElementById('reg-admin-name').value;
            const phone = document.getElementById('reg-admin-phone').value;
            
            if (!username || !password || !name) {
                alert('请填写必填项（用户名、密码、姓名）');
                return;
            }
            
            const result = await fetchData('/admin/register', 'POST', {
                username, password, realName: name, phone, role: 'HOSPITAL_ADMIN'
            });
            
            if (result && (result.success === true || result.adminId || result.id)) {
                closeRegister();
                document.getElementById('register-success').style.display = 'block';
                document.getElementById('login-username').value = username;
                document.getElementById('login-password').value = password;
                setTimeout(() => {
                    document.getElementById('register-success').style.display = 'none';
                }, 3000);
            } else {
                alert(result ? result.message : '注册失败');
            }
        }
        
        function showAddScheduleModal() {
            const today = new Date().toISOString().split('T')[0];
            document.getElementById('add-schedule-date').value = today;
            document.getElementById('add-schedule-slot').value = '';
            document.getElementById('add-schedule-max').value = '20';
            document.getElementById('add-schedule-modal').classList.add('show');
        }
        
        function closeAddScheduleModal() {
            document.getElementById('add-schedule-modal').classList.remove('show');
        }
        
        async function addSchedule() {
            const date = document.getElementById('add-schedule-date').value;
            const slot = document.getElementById('add-schedule-slot').value;
            const max = document.getElementById('add-schedule-max').value;
            
            if (!date || !slot) {
                alert('请填写日期和时间段');
                return;
            }
            
            const doctorData = await fetchData(`/doctors/${currentUserId}`);
            if (!doctorData) {
                alert('无法获取医生信息');
                return;
            }
            
            const result = await fetchData('/schedules', 'POST', {
                hospitalId: doctorData.hospitalId,
                doctorId: currentUserId,
                workDate: date,
                timeSlot: slot,
                maxPatients: parseInt(max) || 20
            });
            
            if (result && result.success) {
                alert('排班添加成功！');
                closeAddScheduleModal();
                loadDoctorScheduleList();
            } else {
                alert(result ? result.message : '添加失败');
            }
        }
        
        async function copyTodaySchedule() {
            const result = await fetchData('/schedules/copy-to-next-day', 'POST');
            if (result && result.success) {
                alert(`成功复制 ${result.copiedCount} 条排班到明天！`);
                loadDoctorScheduleList();
            } else {
                alert(result ? result.message : '复制失败');
            }
        }
        
        async function generateBaseSchedule() {
            const result = await fetchData(`/schedules/generate-base/doctor/${currentUserId}?days=7`, 'POST');
            if (result && result.success) {
                alert(`成功生成 ${result.generatedCount} 条保底排班！`);
                loadDoctorScheduleList();
            } else {
                alert(result ? result.message : '生成失败');
            }
        }
        
        function showAddDoctorModal() {
            fetchData('/hospitals').then(data => {
                const select = document.getElementById('add-doctor-hospital');
                const scoped = currentHospitalId
                    ? data.filter(h => (h.hospitalId || h.id) === currentHospitalId)
                    : data;
                select.innerHTML = '<option value="">请选择医院</option>' +
                    scoped.map(h => `<option value="${h.hospitalId || h.id}">${h.hospitalName || h.name}</option>`).join('');
                if (currentHospitalId) {
                    select.value = currentHospitalId;
                    select.disabled = true;
                } else {
                    select.disabled = false;
                }
            });

            fetchData('/departments').then(data => {
                const select = document.getElementById('add-doctor-dept');
                select.innerHTML = '<option value="">请选择科室</option>' +
                    (data || []).map(d => `<option value="${d.deptId}">${esc(d.name || d.deptName || d.deptId)}</option>`).join('');
            });
            
            document.getElementById('add-doctor-username').value = '';
            document.getElementById('add-doctor-password').value = '';
            document.getElementById('add-doctor-name').value = '';
            document.getElementById('add-doctor-title').value = '';
            document.getElementById('add-doctor-expertise').value = '';
            document.getElementById('add-doctor-fee').value = '50';
            document.getElementById('add-doctor-modal').classList.add('show');
        }
        
        function closeAddDoctorModal() {
            document.getElementById('add-doctor-modal').classList.remove('show');
        }
        
        async function addDoctor() {
            const username = document.getElementById('add-doctor-username').value;
            const password = document.getElementById('add-doctor-password').value;
            const name = document.getElementById('add-doctor-name').value;
            const hospitalId = document.getElementById('add-doctor-hospital').value;
            const deptId = document.getElementById('add-doctor-dept').value;
            const title = document.getElementById('add-doctor-title').value;
            const expertise = document.getElementById('add-doctor-expertise').value;
            const fee = document.getElementById('add-doctor-fee').value;
            
            if (!username || !password || !name || !hospitalId) {
                alert('请填写必填项（用户名、密码、姓名、医院）');
                return;
            }
            
            const result = await fetchData('/doctors', 'POST', {
                username,
                password,
                name,
                hospitalId: hospitalId || currentHospitalId,
                deptId: deptId || 'DEPT001',
                title: title || '主治医师',
                expertise: expertise || '暂无',
                consultationFee: parseInt(fee) || 50,
                status: 1
            });
            
            if (result && result.success) {
                alert('医生添加成功！');
                closeAddDoctorModal();
                loadAdminDoctors();
            } else {
                alert(result ? result.message : '添加失败');
            }
        }
        
        /* ================= 管理员端业务 ================= */
        function todayStr() { return new Date().toISOString().split('T')[0]; }
        function addDays(dateStr, days) {
            const d = new Date(dateStr);
            d.setDate(d.getDate() + days);
            return d.toISOString().split('T')[0];
        }
        function weekStartOf(dateStr) {
            const d = new Date(dateStr || todayStr());
            const day = d.getDay();
            const diff = day === 0 ? -6 : 1 - day;
            d.setDate(d.getDate() + diff);
            return d.toISOString().split('T')[0];
        }
        function fmtMinutes(minutes) {
            const m = Math.max(0, minutes || 0);
            if (m === 0) return '0 分钟';
            if (m % 60 === 0) return (m / 60) + ' 小时';
            return Math.floor(m / 60) + ' 小时 ' + (m % 60) + ' 分钟';
        }
        async function fillDeptSelect(selectId) {
            if (!deptCache.length) {
                deptCache = (await fetchData('/departments')) || [];
            }
            const sel = document.getElementById(selectId);
            if (!sel) return;
            const current = sel.value;
            sel.innerHTML = '<option value="">全部科室</option>' +
                deptCache.map(d => `<option value="${d.deptId}">${esc(d.name || d.deptName || d.deptId)}</option>`).join('');
            sel.value = current || '';
        }
        function pageSlice(items, key, pageSize) {
            const size = pageSize || LIST_PAGE_SIZE;
            const total = (items || []).length;
            const totalPages = Math.max(1, Math.ceil(total / size));
            let page = listPages[key] || 0;
            if (page > totalPages - 1) page = totalPages - 1;
            listPages[key] = page;
            const start = page * size;
            return { slice: (items || []).slice(start, start + size), page, totalPages, total };
        }
        function gotoPage(key, page, rerender) {
            listPages[key] = Math.max(0, page);
            rerender();
        }
        function renderPager(containerId, page, totalPages, onPrev, onNext, totalItems) {
            const el = document.getElementById(containerId);
            if (!el) return;
            if (!totalPages || totalPages <= 1) {
                el.innerHTML = totalItems ? `<span class="page-info">共 ${totalItems} 条</span>` : '';
                return;
            }
            el.innerHTML = `
                <button class="page-btn" ${page <= 0 ? 'disabled' : ''}>上一页</button>
                <span class="page-info">共 ${totalItems || 0} 条 · 第 ${page + 1} / ${totalPages} 页</span>
                <button class="page-btn" ${page >= totalPages - 1 ? 'disabled' : ''}>下一页</button>
            `;
            const buttons = el.querySelectorAll('.page-btn');
            if (buttons[0]) buttons[0].onclick = onPrev;
            if (buttons[1]) buttons[1].onclick = onNext;
        }

        /* ---------- 卫健委：医院准入 ---------- */
        async function loadHospitalFilterOptions() {
            const provinces = await fetchData('/hospitals/provinces');
            document.getElementById('h-filter-province').innerHTML =
                '<option value="">全部省份</option>' +
                (provinces || []).map(p => `<option value="${esc(p)}">${esc(p)}</option>`).join('');
            document.getElementById('h-filter-city').innerHTML = '<option value="">全部城市</option>';
            document.getElementById('h-filter-district').innerHTML = '<option value="">全部区县</option>';
        }
        async function onProvinceChange() {
            const province = document.getElementById('h-filter-province').value;
            const citySel = document.getElementById('h-filter-city');
            document.getElementById('h-filter-district').innerHTML = '<option value="">全部区县</option>';
            if (!province) { citySel.innerHTML = '<option value="">全部城市</option>'; return; }
            const cities = await fetchData('/hospitals/cities?province=' + encodeURIComponent(province));
            citySel.innerHTML = '<option value="">全部城市</option>' +
                (cities || []).map(c => `<option value="${esc(c)}">${esc(c)}</option>`).join('');
        }
        async function onCityChange() {
            const province = document.getElementById('h-filter-province').value;
            const city = document.getElementById('h-filter-city').value;
            const districtSel = document.getElementById('h-filter-district');
            if (!province || !city) { districtSel.innerHTML = '<option value="">全部区县</option>'; return; }
            const districts = await fetchData(`/hospitals/districts?province=${encodeURIComponent(province)}&city=${encodeURIComponent(city)}`);
            districtSel.innerHTML = '<option value="">全部区县</option>' +
                (districts || []).map(d => `<option value="${esc(d)}">${esc(d)}</option>`).join('');
        }
        async function loadAdminHospitals(reset) {
            if (reset) {
                adminHospitalPage = 0;
                await loadHospitalFilterOptions();
            }
            const params = new URLSearchParams();
            params.set('page', adminHospitalPage);
            params.set('size', 8);
            const province = document.getElementById('h-filter-province').value;
            const city = document.getElementById('h-filter-city').value;
            const district = document.getElementById('h-filter-district').value;
            const status = document.getElementById('h-filter-status').value;
            const keyword = document.getElementById('h-search-keyword').value.trim();
            if (province) params.set('province', province);
            if (city) params.set('city', city);
            if (district) params.set('district', district);
            if (status) params.set('status', status);
            if (keyword) params.set('keyword', keyword);

            const res = await fetchData('/admin/hospitals/page?' + params.toString());
            renderAdminHospitals((res && res.content) ? res.content : []);
            renderPager('admin-hospital-pager', adminHospitalPage, (res && res.totalPages) || 0,
                () => { if (adminHospitalPage > 0) { adminHospitalPage -= 1; loadAdminHospitals(false); } },
                () => { adminHospitalPage += 1; loadAdminHospitals(false); },
                (res && res.total) || 0);
        }

        function renderAdminHospitals(list) {
            const container = document.getElementById('admin-hospital-list');
            if (!list.length) { container.innerHTML = '<p class="empty-tip">暂无匹配的医院</p>'; return; }
            const statusMap = {
                0: { text: '待审核', cls: 'status-pending' },
                1: { text: '已启用', cls: 'status-confirmed' },
                2: { text: '已驳回', cls: 'status-canceled' }
            };
            container.innerHTML = list.map(h => {
                const st = statusMap[h.status] || { text: '未知', cls: 'status-pending' };
                const region = [h.province, h.city, h.district].filter(Boolean).join(' / ') || '地区未填写';
                let actions = '';
                if (h.status === 0) {
                    actions = `<a href="#" class="btn btn-success" onclick="auditHospital('${h.hospitalId}', 1); return false;">通过</a>
                               <a href="#" class="btn btn-danger" onclick="auditHospital('${h.hospitalId}', 2); return false;">驳回</a>`;
                } else if (h.status === 1) {
                    actions = `<a href="#" class="btn btn-danger" onclick="auditHospital('${h.hospitalId}', 2); return false;">驳回 / 停用</a>`;
                } else {
                    actions = `<a href="#" class="btn" onclick="auditHospital('${h.hospitalId}', 1); return false;">重新启用</a>`;
                }
                return `<div class="card">
                    <div class="card-title-row"><h3>${esc(h.hospitalName)}</h3><span class="status-badge ${st.cls}">${st.text}</span></div>
                    <p><strong>等级:</strong> ${esc(h.level || '未填写')}</p>
                    <p><strong>地区:</strong> ${esc(region)}</p>
                    <p><strong>地址:</strong> ${esc(h.address || '未填写')}</p>
                    <p><strong>电话:</strong> ${esc(h.phone || '未填写')}</p>
                    <div class="card-actions">${actions}</div>
                </div>`;
            }).join('');
        }
        async function auditHospital(hospitalId, status) {
            let comment = '';
            if (status === 2) {
                comment = prompt('请填写驳回原因（可选）') || '';
            }
            const res = await fetchData(`/admin/hospitals/${hospitalId}/audit`, 'POST', { status, comment });
            if (res && res.success) {
                showToast(res.message, 'success');
                loadAdminHospitals(false);
            } else {
                showToast((res && res.message) || '操作失败', 'error');
            }
        }

        /* ---------- 卫健委：管理员审批 ---------- */
        async function loadAdminAdmins(reset) {
            if (reset) adminAdminPage = 0;
            const params = new URLSearchParams();
            params.set('page', adminAdminPage);
            params.set('size', 8);
            params.set('role', 'HOSPITAL_ADMIN');
            const auditStatus = document.getElementById('a-filter-status').value;
            const keyword = document.getElementById('a-search-keyword').value.trim();
            if (auditStatus !== '') params.set('auditStatus', auditStatus);
            if (keyword) params.set('keyword', keyword);

            const res = await fetchData('/admin/list?' + params.toString());
            renderAdminAdmins((res && res.data) ? res.data : []);
            renderPager('admin-admin-pager', adminAdminPage, (res && res.totalPages) || 0,
                () => { if (adminAdminPage > 0) { adminAdminPage -= 1; loadAdminAdmins(false); } },
                () => { adminAdminPage += 1; loadAdminAdmins(false); },
                (res && res.total) || 0);
        }

        function auditBadge(auditStatus) {
            if (auditStatus === 0) return '<span class="status-badge status-pending">待审核</span>';
            if (auditStatus === 1) return '<span class="status-badge status-confirmed">已通过</span>';
            if (auditStatus === 2) return '<span class="status-badge status-canceled">已驳回</span>';
            return '<span class="status-badge">未知</span>';
        }
        function renderAdminAdmins(list) {
            const container = document.getElementById('admin-admin-list');
            if (!list.length) { container.innerHTML = '<p class="empty-tip">暂无管理员申请</p>'; return; }
            container.innerHTML = list.map(a => `
                <div class="card">
                    <div class="card-title-row"><h3>${esc(a.realName || a.username)}</h3>${auditBadge(a.auditStatus)}</div>
                    <p><strong>用户名:</strong> ${esc(a.username)}</p>
                    <p><strong>手机号:</strong> ${esc(a.phone || '未填写')}</p>
                    <p><strong>所属医院:</strong> ${esc(a.hospitalId || '未绑定')}</p>
                    <div class="card-actions">
                        ${a.auditStatus === 0 ? `<a href="#" class="btn btn-success" onclick="auditAdmin('${a.adminId}', 1); return false;">通过</a>
                        <a href="#" class="btn btn-danger" onclick="auditAdmin('${a.adminId}', 2); return false;">驳回</a>` : ''}
                    </div>
                </div>
            `).join('');
        }
        async function auditAdmin(adminId, auditStatus) {
            let comment = '';
            if (auditStatus === 2) {
                comment = prompt('请填写驳回原因（可选）') || '';
            }
            const res = await fetchData(`/admin/${adminId}/audit`, 'POST', { auditStatus, comment });
            if (res && res.success) {
                showToast(res.message, 'success');
                loadAdminAdmins(false);
            } else {
                showToast((res && res.message) || '操作失败', 'error');
            }
        }

        /* ---------- 医院管理员：本院管理员与申请 ---------- */
        async function loadHospitalAdmins() {
            const res = await fetchData('/admin/list?page=0&size=100');
            hospitalAdminsView = (res && res.data) ? res.data : [];
            listPages.hospitalAdmins = 0;
            renderHospitalAdmins();
        }
        function renderHospitalAdmins() {
            const container = document.getElementById('hospital-admin-list');
            if (!hospitalAdminsView.length) {
                container.innerHTML = '<p class="empty-tip">暂无管理员</p>';
                renderPager('hospital-admin-pager', 0, 0, null, null, 0);
                return;
            }
            const { slice, page, totalPages, total } = pageSlice(hospitalAdminsView, 'hospitalAdmins');
            container.innerHTML = slice.map(a => `
                <div class="card">
                    <div class="card-title-row"><h3>${esc(a.realName || a.username)}</h3>${auditBadge(a.auditStatus)}</div>
                    <p><strong>用户名:</strong> ${esc(a.username)}</p>
                    <p><strong>手机号:</strong> ${esc(a.phone || '未填写')}</p>
                </div>
            `).join('');
            renderPager('hospital-admin-pager', page, totalPages,
                () => gotoPage('hospitalAdmins', page - 1, () => renderHospitalAdmins()),
                () => gotoPage('hospitalAdmins', page + 1, () => renderHospitalAdmins()),
                total);
        }
        function showAdminApplyModal() {
            document.getElementById('apply-admin-username').value = '';
            document.getElementById('apply-admin-password').value = '';
            document.getElementById('apply-admin-name').value = '';
            document.getElementById('apply-admin-phone').value = '';
            document.getElementById('admin-apply-modal').classList.add('show');
        }
        function closeAdminApplyModal() {
            document.getElementById('admin-apply-modal').classList.remove('show');
        }
        async function submitAdminApply() {
            const username = document.getElementById('apply-admin-username').value.trim();
            const password = document.getElementById('apply-admin-password').value;
            const realName = document.getElementById('apply-admin-name').value.trim();
            const phone = document.getElementById('apply-admin-phone').value.trim();
            if (!username || !password || !realName) {
                showToast('请填写用户名、密码和姓名', 'error');
                return;
            }
            const res = await fetchData('/admin/create', 'POST', { username, password, realName, phone });
            if (res && res.success) {
                showToast(res.message, 'success');
                closeAdminApplyModal();
                loadHospitalAdmins();
            } else {
                showToast((res && res.message) || '申请失败', 'error');
            }
        }

        /* ---------- 医院入驻申请（公开） ---------- */
        function showHospitalApplyModal() {
            ['apply-hospital-name', 'apply-hospital-level', 'apply-hospital-province', 'apply-hospital-city',
             'apply-hospital-district', 'apply-hospital-address', 'apply-hospital-phone', 'apply-hospital-description']
                .forEach(id => { document.getElementById(id).value = ''; });
            document.getElementById('hospital-apply-modal').classList.add('show');
        }
        function closeHospitalApplyModal() {
            document.getElementById('hospital-apply-modal').classList.remove('show');
        }
        async function submitHospitalApply() {
            const hospitalName = document.getElementById('apply-hospital-name').value.trim();
            if (!hospitalName) {
                showToast('请填写医院名称', 'error');
                return;
            }
            const body = {
                hospitalName,
                level: document.getElementById('apply-hospital-level').value.trim(),
                province: document.getElementById('apply-hospital-province').value.trim(),
                city: document.getElementById('apply-hospital-city').value.trim(),
                district: document.getElementById('apply-hospital-district').value.trim(),
                address: document.getElementById('apply-hospital-address').value.trim(),
                phone: document.getElementById('apply-hospital-phone').value.trim(),
                description: document.getElementById('apply-hospital-description').value.trim()
            };
            const res = await fetchData('/hospitals/apply', 'POST', body);
            if (res && res.success) {
                showToast(res.message, 'success');
                closeHospitalApplyModal();
            } else {
                showToast((res && res.message) || '提交失败', 'error');
            }
        }

        /* ---------- 医院管理员：科室 ---------- */
        let adminDepartments = [];
        async function loadAdminDepartments() {
            adminDepartments = await fetchData('/departments');
            deptCache = adminDepartments;
            const keyword = document.getElementById('dept-search').value.trim().toLowerCase();
            adminDepartmentsView = keyword
                ? adminDepartments.filter(d => (d.name || d.deptName || '').toLowerCase().includes(keyword))
                : adminDepartments;
            listPages.adminDepts = 0;
            renderAdminDepartments(adminDepartmentsView);
        }
        function renderAdminDepartments(list) {
            const container = document.getElementById('admin-dept-list');
            adminDepartmentsView = list;
            if (!list.length) {
                container.innerHTML = '<p class="empty-tip">暂无科室</p>';
                renderPager('admin-dept-pager', 0, 0, null, null, 0);
                return;
            }
            const { slice, page, totalPages, total } = pageSlice(list, 'adminDepts');
            container.innerHTML = slice.map(d => `
                <div class="card">
                    <h3>${esc(d.name || d.deptName || d.deptId)}</h3>
                    <p><strong>服务时间:</strong> ${esc(d.serviceTime || '未填写')}</p>
                    <p><strong>简介:</strong> ${esc((d.description || '未填写').substring(0, 60))}</p>
                    <p><strong>常见疾病:</strong> ${esc((d.diseases || '未填写').substring(0, 60))}</p>
                    <div class="card-actions">
                        <a href="#" class="btn" onclick="showDeptModal('${d.deptId}'); return false;">编辑</a>
                        <a href="#" class="btn btn-danger" onclick="deleteDepartment('${d.deptId}'); return false;">删除</a>
                    </div>
                </div>
            `).join('');
            renderPager('admin-dept-pager', page, totalPages,
                () => gotoPage('adminDepts', page - 1, () => renderAdminDepartments(adminDepartmentsView)),
                () => gotoPage('adminDepts', page + 1, () => renderAdminDepartments(adminDepartmentsView)),
                total);
        }
        function showDeptModal(deptId) {
            const dept = deptId ? adminDepartments.find(d => d.deptId === deptId) : null;
            document.getElementById('dept-edit-id').value = dept ? dept.deptId : '';
            document.getElementById('dept-modal-title').textContent = dept ? '编辑科室' : '添加科室';
            document.getElementById('dept-name').value = dept ? (dept.name || dept.deptName || '') : '';
            document.getElementById('dept-service-time').value = dept ? (dept.serviceTime || '') : '';
            document.getElementById('dept-description').value = dept ? (dept.description || '') : '';
            document.getElementById('dept-diseases').value = dept ? (dept.diseases || '') : '';
            document.getElementById('dept-modal').classList.add('show');
        }
        function closeDeptModal() {
            document.getElementById('dept-modal').classList.remove('show');
        }
        async function saveDepartment() {
            const id = document.getElementById('dept-edit-id').value;
            const name = document.getElementById('dept-name').value.trim();
            if (!name) { showToast('请填写科室名称', 'error'); return; }
            const body = {
                name,
                serviceTime: document.getElementById('dept-service-time').value.trim(),
                description: document.getElementById('dept-description').value.trim(),
                diseases: document.getElementById('dept-diseases').value.trim()
            };
            const res = id
                ? await fetchData(`/departments/${id}`, 'PUT', body)
                : await fetchData('/departments', 'POST', body);
            if (res && res.success) {
                showToast(id ? '科室已更新' : '科室已添加', 'success');
                closeDeptModal();
                loadAdminDepartments();
            } else {
                showToast((res && res.message) || '保存失败', 'error');
            }
        }
        async function deleteDepartment(deptId) {
            if (!confirm('确定删除该科室吗？')) return;
            await fetchData(`/departments/${deptId}`, 'DELETE');
            loadAdminDepartments();
        }

        /* ---------- 医院管理员：医生 ---------- */
        async function loadAdminDoctors(reset) {
            await fillDeptSelect('admin-doctor-dept');
            const deptId = document.getElementById('admin-doctor-dept').value;
            const url = '/doctors' + (deptId ? '?deptId=' + encodeURIComponent(deptId) : '');
            doctors = await fetchData(url);
            if (reset !== false) listPages.adminDoctors = 0;
            renderAdminDoctors(doctors || []);
        }
        function renderAdminDoctors(list) {
            const container = document.getElementById('admin-doctor-list');
            adminDoctorsView = list;
            if (!list.length) {
                container.innerHTML = '<p class="empty-tip">暂无医生</p>';
                renderPager('admin-doctor-pager', 0, 0, null, null, 0);
                return;
            }
            const { slice, page, totalPages, total } = pageSlice(list, 'adminDoctors');
            container.innerHTML = slice.map(d => `
                <div class="card">
                    <div class="doctor-head">
                        ${doctorAvatarHtml(d, d.name || d.doctorName || '未知医生')}
                        <div class="doctor-head-info">
                            <h3>${esc(d.name || d.doctorName || '未知医生')}</h3>
                            <p class="muted">${esc(d.department || d.deptId || '未填写')} · ${esc(d.title || '未填写')}</p>
                        </div>
                    </div>
                    <p><strong>电话:</strong> ${esc(d.phone || '未填写')}</p>
                    <p><strong>状态:</strong> ${d.status === 1 ? '<span class="status-badge status-confirmed">正常</span>' : '<span class="status-badge status-canceled">停诊</span>'}</p>
                    <div class="card-actions">
                        <a href="#" class="btn" onclick="viewDutyCard('${d.doctorId}'); return false;">信息卡</a>
                        <a href="#" class="btn btn-danger" onclick="deleteDoctor('${d.doctorId}'); return false;">删除</a>
                    </div>
                </div>
            `).join('');
            renderPager('admin-doctor-pager', page, totalPages,
                () => gotoPage('adminDoctors', page - 1, () => renderAdminDoctors(adminDoctorsView)),
                () => gotoPage('adminDoctors', page + 1, () => renderAdminDoctors(adminDoctorsView)),
                total);
        }
        async function deleteDoctor(doctorId) {
            if (!confirm('确定删除该医生吗？')) return;
            await fetchData(`/doctors/${doctorId}`, 'DELETE');
            loadAdminDoctors();
        }

        /* ---------- 医院管理员：出诊规则 ---------- */
        async function loadDutyRulePage() {
            const list = await fetchData('/doctors');
            const sel = document.getElementById('duty-doctor');
            sel.innerHTML = '<option value="">请选择医生</option>' +
                (list || []).map(d => `<option value="${d.doctorId}">${esc(d.name || d.doctorName || d.doctorId)} - ${esc(d.title || '')}</option>`).join('');
            if (!document.getElementById('duty-date').value) {
                document.getElementById('duty-date').value = todayStr();
            }
            loadDutyRules(true);
        }
        async function loadDutyRules(reset) {
            const doctorId = document.getElementById('duty-doctor').value;
            if (!doctorId) {
                dutyRulesView = [];
                listPages.dutyRules = 0;
                renderDutyRules();
                return;
            }
            const date = document.getElementById('duty-date').value || todayStr();
            const start = weekStartOf(date);
            const end = addDays(start, 6);
            dutyRulesView = (await fetchData(`/duty-rules/doctor/${doctorId}?from=${start}&to=${end}`)) || [];
            if (reset !== false) listPages.dutyRules = 0;
            renderDutyRules();
        }
        function renderDutyRules() {
            const container = document.getElementById('duty-rule-list');
            if (!dutyRulesView.length) {
                container.innerHTML = '<p class="empty-tip">本周暂无出诊规则</p>';
                renderPager('duty-rule-pager', 0, 0, null, null, 0);
                return;
            }
            const { slice, page, totalPages, total } = pageSlice(dutyRulesView, 'dutyRules');
            container.innerHTML = slice.map(r => `
                <div class="card">
                    <h3>${esc(r.dutyDate)}</h3>
                    <p><strong>规定时段:</strong> ${esc(r.startTime)} - ${esc(r.endTime)}</p>
                    <p><strong>状态:</strong> ${r.status === 1 ? '生效中' : '已停用'}</p>
                    <div class="card-actions">
                        <a href="#" class="btn btn-danger" onclick="deleteDutyRule('${r.ruleId}'); return false;">删除</a>
                    </div>
                </div>
            `).join('');
            renderPager('duty-rule-pager', page, totalPages,
                () => gotoPage('dutyRules', page - 1, () => renderDutyRules()),
                () => gotoPage('dutyRules', page + 1, () => renderDutyRules()),
                total);
        }
        async function addDutyRule() {
            const doctorId = document.getElementById('duty-doctor').value;
            const dutyDate = document.getElementById('duty-date').value;
            const startTime = document.getElementById('duty-start').value;
            const endTime = document.getElementById('duty-end').value;
            if (!doctorId || !dutyDate || !startTime || !endTime) {
                showToast('请选择医生、日期和时间段', 'error');
                return;
            }
            const res = await fetchData('/duty-rules', 'POST', { doctorId, dutyDate, startTime, endTime });
            if (res && res.success) {
                showToast('出诊规则已添加', 'success');
                loadDutyRules();
            } else {
                showToast((res && res.message) || '添加失败', 'error');
            }
        }
        async function deleteDutyRule(ruleId) {
            if (!confirm('确定删除该出诊规则吗？')) return;
            await fetchData(`/duty-rules/${ruleId}`, 'DELETE');
            loadDutyRules();
        }

        /* ---------- 医院管理员：排班管理 ---------- */
        async function loadAdminSchedule(preferredDoctorId) {
            const sel = document.getElementById('admin-schedule-doctor');
            const previous = sel.value;
            const list = (await fetchData('/doctors')) || [];
            sel.innerHTML = '<option value="">请选择医生</option>' +
                list.map(d => `<option value="${d.doctorId}">${esc(d.name || d.doctorName || d.doctorId)} - ${esc(d.department || d.deptId || '')}</option>`).join('');

            const target = preferredDoctorId || previous || currentScheduleDoctorId || (list[0] && list[0].doctorId);
            if (target) sel.value = target;
            currentScheduleDoctorId = sel.value;

            if (!currentScheduleDoctorId) {
                adminScheduleView = [];
                listPages.adminSchedule = 0;
                renderAdminSchedule();
                return;
            }
            const data = (await fetchData(`/schedules/doctor/${currentScheduleDoctorId}`)) || [];
            adminScheduleView = data.slice().sort((a, b) =>
                String(a.workDate || '').localeCompare(String(b.workDate || '')) ||
                String(a.timeSlot || '').localeCompare(String(b.timeSlot || '')));
            listPages.adminSchedule = 0;
            renderAdminSchedule();
        }
        function renderAdminSchedule() {
            const container = document.getElementById('admin-schedule-list');
            if (!adminScheduleView.length) {
                container.innerHTML = '<p class="empty-tip">该医生暂无排班</p>';
                renderPager('admin-schedule-pager', 0, 0, null, null, 0);
                return;
            }
            const { slice, page, totalPages, total } = pageSlice(adminScheduleView, 'adminSchedule');
            container.innerHTML = slice.map(s => {
                const max = s.maxPatients || 0;
                const booked = s.bookedCount || 0;
                let statusText = '可预约';
                let statusClass = 'status-confirmed';
                if (s.status !== 1) {
                    statusText = '已关闭';
                    statusClass = 'status-canceled';
                } else if (max > 0 && booked >= max) {
                    statusText = '已约满';
                    statusClass = 'status-pending';
                }
                return `<div class="card">
                    <div class="card-title-row"><h3>${esc(s.workDate || '未填写')}</h3>
                        <span class="status-badge ${statusClass}">${statusText}</span></div>
                    <p><strong>时段:</strong> ${esc(s.timeSlot || '未填写')}</p>
                    <p><strong>号源:</strong> ${booked} / ${max}</p>
                </div>`;
            }).join('');
            renderPager('admin-schedule-pager', page, totalPages,
                () => gotoPage('adminSchedule', page - 1, () => renderAdminSchedule()),
                () => gotoPage('adminSchedule', page + 1, () => renderAdminSchedule()),
                total);
        }
        function openDoctorSchedule(doctorId) {
            currentScheduleDoctorId = doctorId;
            closeModal();
            showTab('admin-schedule');
        }

        /* ---------- 医院管理员：在本周在岗达标 ---------- */
        async function loadDutyOverview(backToNow) {
            const input = document.getElementById('overview-week-start');
            if (backToNow || !input.value) {
                input.value = weekStartOf(todayStr());
            }
            await fillDeptSelect('overview-dept');
            const deptId = document.getElementById('overview-dept').value;
            const url = '/admin/doctors/duty-overview?weekStart=' + input.value
                + (deptId ? '&deptId=' + encodeURIComponent(deptId) : '');
            const res = await fetchData(url);
            dutyOverviewView = (res && res.data) ? res.data : [];
            listPages.dutyOverview = 0;
            renderDutyOverview(dutyOverviewView);
        }
        function dutyStatusClass(text) {
            if (!text) return '';
            if (text.indexOf('未达标') >= 0 || text.indexOf('不足') >= 0) return 'status-pending';
            if (text.indexOf('达标') >= 0 || text === '在岗') return 'status-confirmed';
            if (text.indexOf('未') >= 0) return 'status-canceled';
            return '';
        }
        function renderDutyOverview(list) {
            const container = document.getElementById('duty-overview-list');
            dutyOverviewView = list;
            if (!list.length) {
                container.innerHTML = '<p class="empty-tip">该科室暂无医生数据</p>';
                renderPager('duty-overview-pager', 0, 0, null, null, 0);
                return;
            }
            const { slice, page, totalPages, total } = pageSlice(list, 'dutyOverview');
            container.innerHTML = slice.map(d => {
                const week = d.week || {};
                const today = d.today || {};
                const required = week.requiredMinutes || 0;
                const covered = week.coveredMinutes || 0;
                const percent = required > 0 ? Math.min(100, Math.round(covered / required * 100)) : 0;
                return `<div class="card">
                    <div class="doctor-head">
                        ${doctorAvatarHtml(d, d.name || d.doctorId)}
                        <div class="doctor-head-info">
                            <h3>${esc(d.name || d.doctorId)}</h3>
                            <p class="muted">${esc(d.deptName || '未填写')} · ${esc(d.title || '未填写')}</p>
                        </div>
                        <span class="status-badge ${dutyStatusClass(week.statusText)}">${esc(week.statusText || '本周未设规则')}</span>
                    </div>
                    <p><strong>今日:</strong> <span class="status-badge ${dutyStatusClass(today.statusText)}">${esc(today.statusText || '未知')}</span></p>
                    <p><strong>本周时段:</strong> 覆盖 ${fmtMinutes(covered)} / 规定 ${fmtMinutes(required)}</p>
                    <div class="coverage-bar"><div class="coverage-fill" style="width: ${percent}%;"></div></div>
                    <div class="card-actions">
                        <a href="#" class="btn" onclick="viewDutyCard('${d.doctorId}'); return false;">查看信息卡</a>
                    </div>
                </div>`;
            }).join('');
            renderPager('duty-overview-pager', page, totalPages,
                () => gotoPage('dutyOverview', page - 1, () => renderDutyOverview(dutyOverviewView)),
                () => gotoPage('dutyOverview', page + 1, () => renderDutyOverview(dutyOverviewView)),
                total);
        }

        /* ---------- 医生信息卡：基础信息 + 值班数据 ---------- */
        function buildDutyChart(days) {
            const maxReq = Math.max(60, ...days.map(d => d.requiredMinutes || 0));
            const barW = 44, gap = 22, left = 24, top = 12, chartH = 80;
            let content = '';
            days.forEach((d, i) => {
                const x = left + i * (barW + gap);
                const reqH = d.requiredMinutes > 0 ? Math.max(4, (d.requiredMinutes / maxReq) * chartH) : 4;
                const coveredH = d.requiredMinutes > 0 ? Math.min(reqH, (d.coveredMinutes / maxReq) * chartH) : 0;
                const yBase = top + chartH;
                const color = d.onDuty === true ? '#0f766e' : (d.onDuty === false ? '#f59e0b' : '#cbd5e1');
                content += `<rect x="${x}" y="${yBase - reqH}" width="${barW}" height="${reqH}" rx="6" fill="#e2e8f0"></rect>`;
                if (coveredH > 0) {
                    content += `<rect x="${x}" y="${yBase - coveredH}" width="${barW}" height="${coveredH}" rx="6" fill="${color}"></rect>`;
                }
                content += `<text x="${x + barW / 2}" y="${yBase + 16}" text-anchor="middle" font-size="11" fill="#475569">${d.weekday}</text>`;
                const pct = d.requiredMinutes > 0 ? Math.round(d.coveredMinutes / d.requiredMinutes * 100) + '%' : '—';
                content += `<text x="${x + barW / 2}" y="${yBase + 30}" text-anchor="middle" font-size="10" fill="#94a3b8">${pct}</text>`;
            });
            return `<svg viewBox="0 0 560 140" width="100%" height="140" role="img" aria-label="本周值班覆盖">${content}</svg>`;
        }
        async function viewDutyCard(doctorId) {
            const res = await fetchData(`/admin/doctors/${doctorId}/duty-card`);
            if (!res || !res.success) {
                showToast((res && res.message) || '无法获取医生信息卡', 'error');
                return;
            }
            const d = res.data;
            const days = d.days || [];
            const week = d.week || {};
            const today = d.today || {};
            const rows = days.map(day => {
                const ruleText = day.rules && day.rules.length
                    ? day.rules.map(r => esc(r.startTime) + '-' + esc(r.endTime)).join('<br>')
                    : '—';
                const scheduleText = day.schedules && day.schedules.length
                    ? day.schedules.map(s => esc(s.timeSlot)).join('<br>')
                    : '—';
                const cls = day.onDuty === true ? 'status-confirmed' : (day.onDuty === false ? 'status-pending' : '');
                return `<tr>
                    <td>${esc(day.date)}<br><span class="muted">${esc(day.weekday)}</span></td>
                    <td>${ruleText}</td>
                    <td>${scheduleText}</td>
                    <td><span class="status-badge ${cls}">${esc(day.statusText)}</span></td>
                </tr>`;
            }).join('');

            document.getElementById('doctor-detail').innerHTML = `
                <div class="doctor-profile">
                    ${doctorAvatarHtml(d, d.name || d.doctorId, 'avatar-lg')}
                    <div class="doctor-info">
                        <h3>${esc(d.name || d.doctorId)}</h3>
                        <p><strong>职称:</strong> ${esc(d.title || '未填写')}　<strong>科室:</strong> ${esc(d.deptName || '未填写')}</p>
                        <p><strong>电话:</strong> ${esc(d.phone || '未填写')}　<strong>挂号费:</strong> ¥${esc(d.consultationFee || 0)}</p>
                    </div>
                </div>
                <div class="card-actions">
                    <a href="#" class="btn" onclick="openDoctorSchedule('${d.doctorId}'); return false;">查看排班</a>
                </div>
                <div class="info-grid" style="margin-top: 16px;">
                    <div class="info-item"><div class="label">性别</div><div class="value">${d.gender === 'M' ? '男' : (d.gender === 'F' ? '女' : '未填写')}</div></div>
                    <div class="info-item"><div class="label">账号</div><div class="value">${esc(d.username || '—')}</div></div>
                    <div class="info-item"><div class="label">专业擅长</div><div class="value">${esc(d.expertise || '未填写')}</div></div>
                    <div class="info-item"><div class="label">学历</div><div class="value">${esc(d.education || '未填写')}</div></div>
                </div>
                <p style="margin-top: 10px;"><strong>从业经历:</strong> ${esc(d.experience || '未填写')}</p>
                <div class="duty-summary">
                    <div><strong>今日:</strong> <span class="status-badge ${dutyStatusClass(today.statusText)}">${esc(today.statusText || '未知')}</span></div>
                    <div><strong>本周:</strong> <span class="status-badge ${dutyStatusClass(week.statusText)}">${esc(week.statusText || '未设规则')}</span>
                        （覆盖 ${fmtMinutes(week.coveredMinutes)} / 规定 ${fmtMinutes(week.requiredMinutes)}）</div>
                </div>
                <div style="margin-top: 12px;">${buildDutyChart(days)}</div>
                <div class="duty-table-wrap">
                    <table class="duty-table">
                        <thead><tr><th>日期</th><th>规定时段</th><th>实际排班</th><th>在岗</th></tr></thead>
                        <tbody>${rows}</tbody>
                    </table>
                </div>
            `;
            document.getElementById('doctor-modal').classList.add('show');
        }

        window.onclick = function(e) {
            if (e.target.classList.contains('modal')) {
                e.target.classList.remove('show');
            }
        }
        
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && document.getElementById('login-container').style.display !== 'none') {
                login();
            }
        });
