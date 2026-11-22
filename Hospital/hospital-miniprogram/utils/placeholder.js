/**
 * 患者端集中占位数据（Direction A · 暖纸视觉）
 * ------------------------------------------------------------------
 * 只放「设计稿有、后端暂未返回」的字段。禁止在各页面散落编造。
 * 后端接口到位后，只需改本文件（或删除对应键），页面无需改动。
 *
 * 字段与期望来源（后端需补的接口/字段）：
 *   hospital.crestKey        院徽标识           GET /api/hospitals/{id} -> crestKey
 *   hospital.levelText       医院等级（三级甲等） level
 *   hospital.campus          院区名称           campus / branchName
 *   hospital.serviceTime     门诊时间           outpatientTime（如 08:00–17:30）
 *   hospital.switchboard     总机               switchboard / phone
 *   hospital.tags            机构标签            tags[]（公立综合 / 医保定点 / 24h 急诊）
 *   hospital.notice          官方公告            GET /api/hospital-articles?type=NOTICE
 *   dept.group               门诊类型分组         deptGroup（内科/外科/专科/医技）
 *   dept.groupOrder          分组排序            deptGroupSort
 *   dept.todayDoctors        今日出诊数          todayDoctorCount
 *   dept.remaining           今日余号            todayRemaining
 *   dept.floorZone           楼层/诊区           floor + zone
 *   dept.serviceTimeRows     门诊时间明细        clinicSchedule[]
 *   dept.locationHint        院内位置说明        locationHint
 *   dept.phone               科室电话            deptPhone
 *   doctor.titleText         职称                title（已返回时优先用真实值）
 *   doctor.flags             资质标签            flags[]
 *   doctor.remaining         今日余号            todayRemaining
 *   doctor.scheduleDays      本周出诊            GET /api/doctors/{id} -> scheduleDays[]
 *   appointment.queueNo      就诊序号            queueNo
 *   appointment.flowStep     就诊流程当前节点    flowStep（0-4）
 *   appointment.insurance    医保统筹金额        insuranceAmount
 *   appointment.qrPayload    就诊凭证二维码内容   qrPayload
 *   triage.rationale         分诊依据/判断       triageRationale
 *   triage.flowMinutes       预计全程耗时        flowMinutes
 *   triage.goldDecision      金色唯一判断        goldDecision（普通号即可/余号紧张）
 *   search.historyKey        搜索历史 storage key 本地固定
 *   search.hotKeywords       热门搜索            GET /api/search/hot
 *   search.unifiedEndpoint   统一搜索接口        GET /api/search?keyword=&page=&size=&type=
 *   article.publisher        发布单位            article.publisher
 *   article.readCount        阅读数              article.readCount
 *   article.relatedSlots     关联可改约号源        article.relatedSlots[]
 *   patient.family           就诊人列表           GET /api/patients/page（后端未提供，暂 mock）
 *   patient.relation/insurance 与本人关系/医保类型  patients 表扩展字段
 *   navigation.floors/zones/steps 楼层/诊区/引导  GET /api/hospitals/{id}/navigation
 *
 * 分页字段约定（后端同步补齐中，未就绪时按此接）：
 *   page(从 0 开始) / size(默认 20，上限 100) / total / totalPages / hasMore
 *   兼容返回：content | list | rows | records | data，total | totalElements，hasMore | hasNext
 *   详见 utils/pager.js
 */

const HOSPITAL_LEVEL = '三级甲等';

function hashCode(value) {
  const text = String(value || 'H');
  let hash = 0;
  for (let i = 0; i < text.length; i += 1) {
    hash = (hash * 31 + text.charCodeAt(i)) % 997;
  }
  return hash;
}

function pick(list, seed) {
  return list[seed % list.length];
}

/** 医院抬头补充字段 */
function hospitalExtra(hospital) {
  const source = hospital || {};
  return {
    levelText: source.level || HOSPITAL_LEVEL,
    campus: source.campus || '南院区',
    serviceTime: source.outpatientHours || source.serviceTime || '08:00–17:30',
    switchboard: source.guideDeskPhone || source.phone || '029-8763 0000',
    distanceText: source.distanceText || '1.3km',
    tags: source.tags || [
      source.nature || '公立综合',
      source.insuranceSupported ? '医保定点' : '自费',
      source.emergency24h ? '24h 急诊' : '门诊'
    ],
    crestKey: 'shield',
    // 公告只使用后端 /api/home 或文章接口返回的数据，前端不再生成兜底公告
    notice: source.notice || null
  };
}

const GROUP_ORDER = ['内科', '外科', '专科', '医技'];

const DEPT_GROUP_RULES = [
  { group: '内科', keywords: ['内', '呼吸', '心', '消化', '神经', '肾', '内分泌', '血液', '风湿'] },
  { group: '外科', keywords: ['外科', '骨科', '泌尿', '心胸', '整形', '烧伤'] },
  { group: '专科', keywords: ['眼科', '耳鼻', '口腔', '皮肤', '妇产', '儿科', '中医', '康复', '精神', '感染'] },
  { group: '医技', keywords: ['检验', '影像', '药剂', '超声', '病理', '营养'] }
];

/** 科室分组 / 出诊数 / 余号 / 位置 补充字段 */
function departmentExtra(dept) {
  const name = (dept && dept.deptName) || '';
  const seed = hashCode((dept && dept.deptId) || name);

  let group = '专科';
  DEPT_GROUP_RULES.forEach(rule => {
    if (rule.keywords.some(keyword => name.indexOf(keyword) >= 0)) {
      if (group === '专科' || rule.group !== '专科') {
        group = rule.group;
      }
    }
  });

  const todayDoctors = Number(dept && dept.todayDoctorCount);
  const remaining = Number(dept && dept.todayRemaining);
  const serviceTime = String((dept && dept.serviceTime) || '').trim();

  return {
    group: (dept && dept.group) || group,
    groupOrder: GROUP_ORDER.indexOf(group),
    todayDoctors: todayDoctors > 0 ? todayDoctors : 3 + (seed % 3),
    remaining: remaining > 0 ? remaining : 4 + (seed % 9),
    floorZone: (dept && (dept.location || dept.floorZone)) ||
      ((1 + seed % 4) + ' 层 · ' + pick(['A 区', 'B 区', 'C 区'], seed)),
    locationHint: (dept && dept.locationHint) || '电梯直达后按楼层指引前往诊区，导医台可问询',
    phone: (dept && dept.phone) || (dept && dept.guideInfo && dept.guideInfo.phone) || '029-8763 0126',
    // 门诊时间只展示后端 service_time 的内容，不再生成模板排班
    serviceTimeRows: serviceTime
      ? serviceTime.split('\n').map(line => line.trim()).filter(Boolean)
        .map(line => ({ label: '门诊时间', detail: line, value: '' }))
      : []
  };
}

/** 医生卡补充字段 */
function doctorExtra(doctor) {
  const remaining = Number(doctor && doctor.todayRemaining);
  return {
    titleText: (doctor && (doctor.title || doctor.doctorTitle)) || '',
    flags: (doctor && doctor.flags) || (doctor && doctor.tags ? [doctor.tags] : []),
    remaining: remaining > 0 ? remaining : 0
  };
}

/** 预约详情补充字段 */
function appointmentExtra(appointment) {
  const seed = hashCode(appointment && appointment.appointmentId);
  return {
    queueNo: '第 ' + (10 + seed % 28) + ' 号',
    flowStep: 0,
    insurance: 30,
    qrPayload: '',
    appointmentNo: '1024 1187 ' + String(1000 + seed % 8999).slice(0, 4)
  };
}

/** 分诊结果补充字段 */
function triageExtra(result) {
  return {
    flowMinutes: 50,
    goldDecision: (result && result.goldDecision) || '普通号即可',
    rationale: (result && result.rationale) ||
      '症状单一、无发热与呼吸困难，无需挂专家号。若咳嗽超过 8 周或出现痰中带血，请复诊并申请胸部 CT。',
    evidence: (result && result.evidence) || []
  };
}

/** 搜索：历史本地存最近 8 条 */
const SEARCH_HISTORY_KEY = 'searchHistory';

function getSearchHistory() {
  try {
    return wx.getStorageSync(SEARCH_HISTORY_KEY) || [];
  } catch (e) {
    return [];
  }
}

function saveSearchHistory(keyword) {
  const trimmed = String(keyword || '').trim();
  if (!trimmed) return getSearchHistory();
  const history = getSearchHistory().filter(item => item !== trimmed);
  history.unshift(trimmed);
  const next = history.slice(0, 8);
  try {
    wx.setStorageSync(SEARCH_HISTORY_KEY, next);
  } catch (e) { /* 存储不可用时忽略 */ }
  return next;
}

function clearSearchHistory() {
  try {
    wx.removeStorageSync(SEARCH_HISTORY_KEY);
  } catch (e) { /* ignore */ }
  return [];
}

const HOT_KEYWORDS = ['咳嗽挂什么科', '眼科 王敏', '儿科发热', '医保报销', '停诊通知'];

/** 公告条目补充字段：发布单位 / 阅读数 / 相关号源（GET /api/hospital-articles/page 字段就绪后替换） */
function articleExtra(article) {
  return {
    publisher: (article && article.publisher) || '',
    publishDate: String((article && (article.publishedAt || article.createTime)) || '').replace('T', ' ').slice(0, 10),
    readCount: Number(article && article.readCount) || 0,
    relatedSlots: (article && article.relatedSlots) || [],
    // 咨询电话：后端文章接口暂无 phone 字段，空值由页面显示「—」
    phone: (article && article.phone) || ''
  };
}

const FAMILY_STORAGE_KEY = 'patientFamily';

/** 就诊人列表（后端暂无 /api/patients/page，仅本人 + 家庭成员 mock；字段：patientId/name/gender/age/idMasked/relation/insurance/tags/default） */
function familyPatients(primary) {
  const base = primary && (primary.patientName || primary.name);
  let saved = [];
  try {
    saved = wx.getStorageSync(FAMILY_STORAGE_KEY) || [];
  } catch (e) { saved = []; }

  const self = base
    ? {
        patientId: (primary && primary.patientId) || 'SELF',
        name: base,
        gender: (primary && primary.gender) === 2 || (primary && primary.gender) === 'F' ? '女' : '男',
        age: Number(primary && primary.age) || 32,
        idMasked: (primary && primary.idCard) ? maskId(primary.idCard) : '6101****1234',
        relation: '本人',
        insurance: '职工医保',
        tags: ['默认就诊人'],
        isSelf: true,
        default: true
      }
    : null;

  const mock = [
    {
      patientId: 'F001',
      name: '王秀兰',
      gender: '女',
      age: 65,
      idMasked: '6101****4426',
      relation: '父母',
      insurance: '居民医保',
      tags: ['长辈模式已开启'],
      isSelf: false,
      default: false
    },
    {
      patientId: 'F002',
      name: '李思远',
      gender: '男',
      age: 8,
      idMasked: '6101****7712',
      relation: '子女',
      insurance: '儿童医保',
      tags: ['需监护人陪同'],
      isSelf: false,
      default: false
    }
  ];

  const list = [];
  if (self) list.push(self);
  mock.forEach(item => {
    const hit = saved.find(p => p.patientId === item.patientId);
    list.push(hit ? Object.assign({}, item, hit) : item);
  });
  return list;
}

function maskId(idCard) {
  const text = String(idCard || '').replace(/\s/g, '');
  if (text.length < 8) return text || '6101****1234';
  return text.slice(0, 4) + '****' + text.slice(-4);
}

/** 院内导航 mock（后端无楼层/诊区坐标，接口到位后按 floor/zones/steps 替换） */
function navigationMock(hospital) {
  return {
    building: '门诊楼',
    floorText: '3 层',
    minutes: 6,
    floors: [
      { key: '1F', name: '1F 大厅', zones: [] },
      { key: '2F', name: '2F 外科', zones: [] },
      { key: '3F', name: '3F 内科', zones: [] },
      { key: '4F', name: '4F 眼科', zones: [] }
    ],
    zones: [
      { name: '候诊大厅', left: 6, top: 6, width: 40, height: 18, active: false },
      { name: '护士分诊台', left: 55, top: 6, width: 39, height: 18, active: false },
      { name: '电梯厅', left: 38, top: 36, width: 24, height: 13, active: false },
      { name: 'A 区 · 呼吸内科', left: 8, top: 36, width: 26, height: 26, active: true },
      { name: 'B 区 · 眼科', left: 66, top: 36, width: 28, height: 26, active: false },
      { name: '检验科', left: 8, top: 66, width: 34, height: 22, active: false },
      { name: '收费 / 盖章', left: 66, top: 66, width: 28, height: 22, active: false }
    ],
    steps: [
      { title: '从候诊大厅出发', desc: '面向护士分诊台，向右侧电梯厅方向走。' },
      { title: '乘电梯至 3 层', desc: '出电梯后左转，沿走廊直行约 20 米。' },
      { title: '到达 A 区呼吸内科', desc: '先在分诊台报到，再到 6 诊室候诊。' }
    ],
    services: [
      { name: '导医台', detail: '1F 大厅' },
      { name: '咨询电话', detail: '029-8763 0000' },
      { name: '轮椅借用', detail: '1F 服务台' }
    ],
    hospitalName: (hospital && hospital.hospitalName) || '',
    campus: (hospital && hospital.campus) || '南院区'
  };
}

module.exports = {
  articleExtra,
  familyPatients,
  navigationMock,
  hospitalExtra,
  departmentExtra,
  doctorExtra,
  appointmentExtra,
  triageExtra,
  groupOrder: GROUP_ORDER,
  getSearchHistory,
  saveSearchHistory,
  clearSearchHistory,
  hotKeywords: HOT_KEYWORDS,
  searchHistoryKey: SEARCH_HISTORY_KEY
};
