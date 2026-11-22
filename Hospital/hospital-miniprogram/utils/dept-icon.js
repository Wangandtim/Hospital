/**
 * 科室名称 → 图标 key
 * 权威映射：2026-11-22/dept-icons/mapping.json
 * 规则：仅认映射表中的科室名与别名；映射表以外的写法一律落到 fallback，不做猜测。
 */
const DEPT_ICONS = {
  // 当前库中 29 种科室名称
  '口腔科': 'dental',
  '神经内科': 'neurology',
  '急诊科': 'emergency',
  '肿瘤科': 'oncology',
  '心血管内科': 'cardiology',
  '儿科': 'pediatrics',
  '骨科': 'orthopedics',
  '皮肤科': 'dermatology',
  '妇产科': 'obstetrics',
  '耳鼻喉科': 'ent',
  '眼科': 'ophthalmology',
  '呼吸内科': 'respiratory',
  '内分泌科': 'endocrinology',
  '重症医学科': 'icu',
  '麻醉科': 'anesthesia',
  '老年病科': 'geriatrics',
  '内科': 'internal-medicine',
  '检验科': 'laboratory',
  '外科': 'surgery',
  '超声科': 'ultrasound',
  '消化内科': 'gastroenterology',
  '药剂科': 'pharmacy',
  '康复医学科': 'rehabilitation',
  '放射科': 'radiology',
  '普外科': 'general-surgery',
  '肾内科': 'nephrology',
  '中医科': 'tcm',
  '预防保健科': 'prevention',
  '演示综合门诊': 'fallback',
  // 别名
  '耳鼻咽喉科': 'ent',
  '影像科': 'radiology',
  '医学影像科': 'radiology',
  '普通外科': 'general-surgery',
  // 备选（库中暂无，出现时可直接对应）
  '泌尿外科': 'urology',
  '精神心理科': 'psychiatry',
  '病理科': 'pathology',
  '输血科': 'blood-transfusion'
};

const FALLBACK = 'fallback';

function keyFor(deptName) {
  const name = String(deptName || '').trim();
  return DEPT_ICONS[name] || FALLBACK;
}

function iconPath(deptName) {
  return '/images/dept-icons/' + keyFor(deptName) + '.png';
}

module.exports = { keyFor, iconPath, FALLBACK };
