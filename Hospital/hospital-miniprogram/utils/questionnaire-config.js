// 三端共用的科室问卷配置与解析器
// 患者端提交格式：{"q1":"...","q2":["..."]}（扁平）
// 兼容历史/其他端的格式：{"deptId":"DEPT001","answers":{"q1":"..."}}（嵌套）

const QUESTIONNAIRES = {
  'DEPT001': {
    deptName: '急诊科',
    questions: [
      { id: 'q1', type: 'radio', label: '症状出现多久了？', options: ['刚发生（1小时内）', '几小时内', '半天内', '1天以上'] },
      { id: 'q2', type: 'radio', label: '是否有严重症状？', options: ['剧烈胸痛', '呼吸困难', '大量出血', '意识不清', '以上都没有'] },
      { id: 'q3', type: 'textarea', label: '请描述具体症状（选填）' }
    ]
  },
  'DEPT002': {
    deptName: '心血管内科',
    questions: [
      { id: 'q1', type: 'radio', label: '之前是否确诊过心血管疾病？', options: ['是', '否'] },
      { id: 'q2', type: 'checkbox', label: '目前有哪些症状？', options: ['胸闷', '胸痛', '心悸', '气短', '头晕', '其他'] },
      { id: 'q3', type: 'radio', label: '血压情况？', options: ['正常', '偏高', '偏低', '不清楚'] },
      { id: 'q4', type: 'textarea', label: '其他说明（选填）' }
    ]
  },
  'DEPT003': {
    deptName: '消化内科',
    questions: [
      { id: 'q1', type: 'radio', label: '症状持续多长时间？', options: ['1-3天', '一周内', '一个月内', '一个月以上'] },
      { id: 'q2', type: 'checkbox', label: '有哪些症状？', options: ['腹痛', '腹胀', '恶心呕吐', '腹泻', '便秘', '食欲不振', '其他'] },
      { id: 'q3', type: 'radio', label: '大便颜色是否异常？', options: ['正常', '黑色', '带血', '白色', '不清楚'] },
      { id: 'q4', type: 'textarea', label: '请描述具体症状（选填）' }
    ]
  },
  'DEPT004': {
    deptName: '骨科',
    questions: [
      { id: 'q1', type: 'radio', label: '受伤原因？', options: ['跌倒', '车祸', '运动损伤', '重物砸伤', '其他'] },
      { id: 'q2', type: 'radio', label: '受伤部位是否可以活动？', options: ['可以正常活动', '活动受限', '完全不能活动'] },
      { id: 'q3', type: 'radio', label: '是否有明显肿胀或变形？', options: ['是', '否', '不确定'] },
      { id: 'q4', type: 'textarea', label: '请描述具体情况（选填）' }
    ]
  },
  'DEPT005': {
    deptName: '神经内科',
    questions: [
      { id: 'q1', type: 'checkbox', label: '有哪些症状？', options: ['头痛', '头晕', '肢体麻木', '肢体无力', '语言障碍', '视力模糊', '其他'] },
      { id: 'q2', type: 'radio', label: '症状持续多久了？', options: ['几分钟', '几小时', '几天', '持续存在'] },
      { id: 'q3', type: 'radio', label: '是否突然发作？', options: ['突然发作', '逐渐加重', '时好时坏'] },
      { id: 'q4', type: 'textarea', label: '其他说明（选填）' }
    ]
  },
  'DEPT006': {
    deptName: '呼吸内科',
    questions: [
      { id: 'q1', type: 'checkbox', label: '有哪些症状？', options: ['咳嗽', '咳痰', '发热', '胸闷', '气喘', '咽痛', '其他'] },
      { id: 'q2', type: 'radio', label: '症状持续多长时间？', options: ['3天以内', '一周以内', '半个月以内', '一个月以上'] },
      { id: 'q3', type: 'radio', label: '是否有吸烟史？', options: ['从不吸烟', '偶尔吸烟', '经常吸烟', '已戒烟'] },
      { id: 'q4', type: 'textarea', label: '请描述具体情况（选填）' }
    ]
  },
  'DEPT007': {
    deptName: '儿科',
    questions: [
      { id: 'q1', type: 'radio', label: '患儿年龄？', options: ['0-1岁', '1-3岁', '3-6岁', '6岁以上'] },
      { id: 'q2', type: 'radio', label: '是否有发热？', options: ['是（38.5℃以上）', '是（38.5℃以下）', '否'] },
      { id: 'q3', type: 'checkbox', label: '有哪些症状？', options: ['咳嗽', '流涕', '呕吐', '腹泻', '皮疹', '食欲不振', '哭闹不止', '其他'] },
      { id: 'q4', type: 'textarea', label: '请描述具体情况（选填）' }
    ]
  },
  'DEPT008': {
    deptName: '妇产科',
    questions: [
      { id: 'q1', type: 'radio', label: '主要就诊目的？', options: ['妇科检查', '妇科疾病', '产前检查', '产后复查', '计划生育'] },
      { id: 'q2', type: 'radio', label: '末次月经时间？', options: ['一个月内', '1-2个月前', '2个月以上前', '已绝经', '不愿透露'] },
      { id: 'q3', type: 'checkbox', label: '有哪些不适症状？', options: ['腹痛', '白带异常', '月经不调', '瘙痒', '肿块', '无明显症状', '其他'] },
      { id: 'q4', type: 'textarea', label: '请描述具体情况（选填）' }
    ]
  },
  'DEPT009': {
    deptName: '皮肤科',
    questions: [
      { id: 'q1', type: 'radio', label: '症状持续多长时间？', options: ['1-3天', '一周内', '一个月内', '一个月以上'] },
      { id: 'q2', type: 'checkbox', label: '有哪些症状？', options: ['瘙痒', '红斑', '丘疹', '水疱', '脱皮', '色素沉着', '其他'] },
      { id: 'q3', type: 'radio', label: '是否接触过特殊物质？', options: ['是', '否', '不确定'] },
      { id: 'q4', type: 'radio', label: '是否有过敏史？', options: ['有', '没有', '不清楚'] },
      { id: 'q5', type: 'textarea', label: '请描述具体情况（选填）' }
    ]
  },
  'DEPT010': {
    deptName: '眼科',
    questions: [
      { id: 'q1', type: 'checkbox', label: '有哪些症状？', options: ['视力下降', '眼痛', '眼痒', '眼红', '分泌物增多', '怕光', '视物模糊', '其他'] },
      { id: 'q2', type: 'radio', label: '症状持续多长时间？', options: ['1-3天', '一周内', '一个月内', '一个月以上'] },
      { id: 'q3', type: 'radio', label: '是否有眼部外伤史？', options: ['是', '否'] },
      { id: 'q4', type: 'radio', label: '是否佩戴眼镜或隐形眼镜？', options: ['不戴', '框架眼镜', '隐形眼镜', '两者都戴'] },
      { id: 'q5', type: 'textarea', label: '请描述具体情况（选填）' }
    ]
  },
  'DEPT011': {
    deptName: '耳鼻喉科',
    questions: [
      { id: 'q1', type: 'checkbox', label: '有哪些症状？', options: ['鼻塞', '流涕', '打喷嚏', '咽痛', '声音嘶哑', '耳痛', '耳鸣', '听力下降', '其他'] },
      { id: 'q2', type: 'radio', label: '症状持续多长时间？', options: ['1-3天', '一周内', '一个月内', '一个月以上'] },
      { id: 'q3', type: 'radio', label: '是否有发热？', options: ['是（38℃以上）', '是（38℃以下）', '否'] },
      { id: 'q4', type: 'radio', label: '是否有呼吸困难？', options: ['是', '否', '偶尔'] },
      { id: 'q5', type: 'textarea', label: '请描述具体情况（选填）' }
    ]
  },
  'DEPT012': {
    deptName: '口腔科',
    questions: [
      { id: 'q1', type: 'checkbox', label: '有哪些症状？', options: ['牙痛', '牙龈出血', '口腔溃疡', '牙齿松动', '口臭', '咀嚼困难', '其他'] },
      { id: 'q2', type: 'radio', label: '症状持续多长时间？', options: ['1-3天', '一周内', '一个月内', '一个月以上'] },
      { id: 'q3', type: 'radio', label: '疼痛性质？', options: ['持续性痛', '阵发性痛', '咬合时痛', '自发痛', '其他'] },
      { id: 'q4', type: 'radio', label: '是否影响进食？', options: ['严重影响', '轻微影响', '不影响'] },
      { id: 'q5', type: 'textarea', label: '请描述具体情况（选填）' }
    ]
  },
  'DEPT013': {
    deptName: '内分泌科',
    questions: [
      { id: 'q1', type: 'radio', label: '是否有糖尿病史？', options: ['是', '否', '不清楚'] },
      { id: 'q2', type: 'radio', label: '血糖控制情况？', options: ['控制良好', '一般', '较差', '不清楚'] },
      { id: 'q3', type: 'checkbox', label: '有哪些症状？', options: ['多饮', '多尿', '多食', '体重下降', '乏力', '视力模糊', '其他'] },
      { id: 'q4', type: 'radio', label: '近半年体重变化？', options: ['增加5kg以上', '基本不变', '减少5kg以上', '不清楚'] },
      { id: 'q5', type: 'textarea', label: '请描述具体情况（选填）' }
    ]
  },
  'DEPT014': {
    deptName: '肾内科',
    questions: [
      { id: 'q1', type: 'radio', label: '是否有浮肿？', options: ['是', '否', '偶尔'] },
      { id: 'q2', type: 'radio', label: '尿量是否变化？', options: ['明显增多', '明显减少', '基本正常', '不清楚'] },
      { id: 'q3', type: 'radio', label: '尿液颜色？', options: ['正常', '发红', '浑浊', '泡沫多', '不清楚'] },
      { id: 'q4', type: 'radio', label: '是否有高血压病史？', options: ['是', '否', '不清楚'] },
      { id: 'q5', type: 'textarea', label: '请描述具体情况（选填）' }
    ]
  },
  'DEPT015': {
    deptName: '血液科',
    questions: [
      { id: 'q1', type: 'checkbox', label: '有哪些症状？', options: ['乏力', '头晕', '面色苍白', '发热', '出血倾向', '骨痛', '其他'] },
      { id: 'q2', type: 'radio', label: '症状持续多长时间？', options: ['一周内', '一个月内', '三个月内', '三个月以上'] },
      { id: 'q3', type: 'radio', label: '是否有出血倾向？', options: ['经常鼻出血', '牙龈出血', '皮肤瘀斑', '无', '不清楚'] },
      { id: 'q4', type: 'radio', label: '近三个月体重变化？', options: ['增加', '减少5kg以上', '基本不变', '不清楚'] },
      { id: 'q5', type: 'textarea', label: '请描述具体情况（选填）' }
    ]
  },
  'DEPT016': {
    deptName: '风湿免疫科',
    questions: [
      { id: 'q1', type: 'checkbox', label: '关节有哪些症状？', options: ['疼痛', '肿胀', '僵硬', '活动受限', '畸形', '无', '其他'] },
      { id: 'q2', type: 'radio', label: '晨僵持续时间？', options: ['小于30分钟', '30分钟-1小时', '1小时以上', '无晨僵'] },
      { id: 'q3', type: 'radio', label: '是否有皮疹？', options: ['是', '否', '偶尔'] },
      { id: 'q4', type: 'radio', label: '家族中是否有风湿病患者？', options: ['是', '否', '不清楚'] },
      { id: 'q5', type: 'textarea', label: '请描述具体情况（选填）' }
    ]
  },
  'DEPT017': {
    deptName: '内科',
    questions: [
      { id: 'q1', type: 'radio', label: '主要有哪些不适？', options: ['头晕乏力', '咳嗽发热', '腹痛腹泻', '胸闷心悸', '其他'] },
      { id: 'q2', type: 'radio', label: '症状持续多长时间？', options: ['1-3天', '一周内', '一个月内', '一个月以上'] },
      { id: 'q3', type: 'radio', label: '是否伴有发热？', options: ['是（38℃以上）', '是（38℃以下）', '否'] },
      { id: 'q4', type: 'textarea', label: '其他需要补充的情况（选填）' }
    ]
  },
  'DEPT018': {
    deptName: '预防保健科',
    questions: [
      { id: 'q1', type: 'radio', label: '本次就诊需求？', options: ['健康体检', '疫苗接种', '慢病随访', '健康咨询', '其他'] },
      { id: 'q2', type: 'radio', label: '最近一次体检时间？', options: ['半年内', '半年至1年', '1-2年前', '2年以上', '未体检'] },
      { id: 'q3', type: 'checkbox', label: '既往确诊的慢性病？', options: ['高血压', '糖尿病', '冠心病', '慢性肾病', '无', '其他'] },
      { id: 'q4', type: 'textarea', label: '其他需要补充的情况（选填）' }
    ]
  }
};

const DEFAULT_QUESTIONNAIRE = {
  deptName: '普通门诊',
  questions: [
    { id: 'q1', type: 'radio', label: '症状持续多长时间？', options: ['1-3天', '一周内', '一个月内', '一个月以上'] },
    { id: 'q2', type: 'radio', label: '之前是否有类似症状？', options: ['是', '否'] },
    { id: 'q3', type: 'textarea', label: '请描述您的症状（选填）' }
  ]
};

// 科室编号在不同医院并不固定（H001_DEPT001 可能是内分泌科，H002_DEPT001 可能是内科，
// 新增科室又可能是 D1234），所以一律以「科室名称」优先匹配，编号只作兜底。
const NAME_TO_KEY = {};
Object.keys(QUESTIONNAIRES).forEach(key => {
  NAME_TO_KEY[QUESTIONNAIRES[key].deptName] = key;
});
// 常见别名，覆盖不同医院的叫法
Object.assign(NAME_TO_KEY, {
  '耳鼻咽喉科': 'DEPT011',
  '耳鼻咽喉头颈外科': 'DEPT011',
  '风湿科': 'DEPT016',
  '风湿免疫科': 'DEPT016',
  '口腔内科': 'DEPT012',
  '内分泌代谢科': 'DEPT013',
  '肾脏内科': 'DEPT014',
  '血液内科': 'DEPT015',
  '呼吸与危重症医学科': 'DEPT006',
  '消化科': 'DEPT003',
  '神经科': 'DEPT005',
  '小儿科': 'DEPT007',
  '创伤骨科': 'DEPT004'
});

function normalizeDeptKey(deptId) {
  if (!deptId) return '';
  const key = String(deptId);
  const segment = key.indexOf('_') === -1 ? key : key.split('_').pop();
  return segment;
}

function getConfig(deptId, deptName) {
  const nameKey = deptName ? NAME_TO_KEY[String(deptName).trim()] : null;
  if (nameKey) {
    return QUESTIONNAIRES[nameKey];
  }
  return QUESTIONNAIRES[normalizeDeptKey(deptId)] || DEFAULT_QUESTIONNAIRE;
}

function answerText(answer) {
  if (Array.isArray(answer)) {
    return answer.join('、');
  }
  if (answer === undefined || answer === null) {
    return '';
  }
  return String(answer);
}

function isAnswered(answer) {
  if (answer === undefined || answer === null) return false;
  if (Array.isArray(answer)) return answer.length > 0;
  return String(answer).trim() !== '';
}

/**
 * 解析问卷 JSON，兼容扁平 {q1:...} 与嵌套 {deptId, answers:{...}}。
 * 返回 { deptName, title, answers:[{question, answer}] }，解析失败返回 null。
 */
function parse(raw, deptId, deptName) {
  if (!raw) return null;
  let parsed = raw;
  if (typeof raw === 'string') {
    try {
      parsed = JSON.parse(raw);
    } catch (e) {
      return null;
    }
  }
  if (!parsed || typeof parsed !== 'object') return null;

  const answersSource = (parsed.answers && typeof parsed.answers === 'object') ? parsed.answers : parsed;
  const config = getConfig(deptId || parsed.deptId, deptName);
  const displayDeptName = deptName || config.deptName;
  const answers = [];
  const used = {};

  (config.questions || []).forEach(q => {
    const answer = answersSource[q.id];
    if (isAnswered(answer)) {
      used[q.id] = true;
      answers.push({ question: q.label, answer: answerText(answer) });
    }
  });

  // 题目配置与问卷不匹配时，按原始键兜底，避免医生端看到空白
  Object.keys(answersSource).forEach(key => {
    if (used[key] || key === 'deptId' || key === 'answers') return;
    const answer = answersSource[key];
    if (!isAnswered(answer)) return;
    answers.push({ question: '问题' + key.replace(/^q/i, ''), answer: answerText(answer) });
  });

  return {
    deptName: displayDeptName,
    title: displayDeptName + '健康问卷',
    answers: answers
  };
}

module.exports = {
  QUESTIONNAIRES,
  DEFAULT_QUESTIONNAIRE,
  getConfig,
  parse
};
