package com.hospital.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 分诊规则引擎（确定性、可审计）。
 *
 * 只做三件事：红旗硬拦截、科室映射、置信度与专家判定。
 * 不查库、不依赖网络，输入症状文本与画像，输出结构化结论，便于单测与回归。
 * 规则版本：triage-rules-2026.11-v1。
 */
@Service
public class TriageRuleEngine {

    public static final String RULE_VERSION = "triage-rules-2026.11-v1";

    private static final String[] GENERIC_WORDS = {"不舒服", "难受", "不适", "有点难受", "不得劲"};

    /** 科室关键词：key=科室名，value=关键词->权重（2 为强关键词）。 */
    private static final Map<String, Map<String, Integer>> DEPT_RULES = new LinkedHashMap<>();

    static {
        DEPT_RULES.put("呼吸内科", keywords(
                strong("咳嗽", "发热", "气喘", "支气管", "肺炎"),
                normal("咳痰", "低烧", "咽痛", "鼻塞", "流涕", "气短")));
        DEPT_RULES.put("消化内科", keywords(
                strong("胃痛", "反酸", "腹泻", "呕吐"),
                normal("烧心", "恶心", "便秘", "腹胀", "消化不良", "上腹痛", "胃不舒服", "拉肚子",
                        "腹痛", "没胃口", "嗳气")));
        DEPT_RULES.put("心血管内科", keywords(
                strong("心慌", "心悸", "胸闷", "高血压"),
                normal("心律不齐", "水肿", "气短", "心口疼")));
        DEPT_RULES.put("神经内科", keywords(
                strong("头痛", "头晕", "眩晕", "面瘫", "偏头痛"),
                normal("失眠", "手麻", "记忆减退", "头昏", "脑袋疼")));
        DEPT_RULES.put("内分泌科", keywords(
                strong("血糖", "糖尿病", "甲状腺"),
                normal("口渴", "多尿", "体重下降", "甲亢", "甲减")));
        DEPT_RULES.put("肾内科", keywords(
                strong("尿频", "尿急", "尿痛", "血尿"),
                normal("泡沫尿", "肾", "水肿", "排尿困难")));
        DEPT_RULES.put("骨科", keywords(
                strong("腰痛", "关节痛", "骨折", "扭伤"),
                normal("颈肩痛", "膝盖", "背痛", "腿痛", "骨头疼")));
        DEPT_RULES.put("外科", keywords(
                strong("右下腹痛", "阑尾", "疝气", "肿块", "便血"),
                normal("伤口", "肛周", "包块")));
        DEPT_RULES.put("妇产科", keywords(
                strong("月经", "白带", "怀孕", "产检"),
                normal("下腹痛", "孕期", "妇科", "分泌物", "腹痛")));
        DEPT_RULES.put("儿科", keywords(
                strong("儿童", "宝宝", "孩子", "婴儿"),
                normal("小朋友", "小儿")));
        DEPT_RULES.put("眼科", keywords(
                strong("眼痛", "眼红", "视力"),
                normal("流泪", "视物模糊", "眼睛")));
        DEPT_RULES.put("耳鼻喉科", keywords(
                strong("耳鸣", "耳痛", "听力", "扁桃体"),
                normal("鼻窦", "声音嘶哑", "耳朵")));
        DEPT_RULES.put("口腔科", keywords(
                strong("牙痛", "牙龈", "智齿"),
                normal("口腔溃疡", "牙齿")));
        DEPT_RULES.put("皮肤科", keywords(
                strong("皮疹", "瘙痒", "湿疹"),
                normal("红斑", "水疱", "脱发", "皮肤")));
        DEPT_RULES.put("肿瘤科", keywords(
                strong("肿瘤", "癌", "化疗", "放疗"),
                normal("结节")));
        DEPT_RULES.put("内科", keywords(strong(),
                normal("乏力", "低热", "浑身没劲", "没力气", "浑身疼")));
        DEPT_RULES.put("老年病科", keywords(strong(),
                normal("老年", "上了年纪")));
    }

    private static final Set<String> EXCLUDED_DEPTS = new LinkedHashSet<>();

    static {
        Collections.addAll(EXCLUDED_DEPTS,
                "急诊科", "放射科", "检验科", "药剂科", "超声科", "重症医学科", "麻醉科");
    }

    /** 需要怀孕状态才能定科室的触发词（女性 + 未知孕产 → 封顶 MEDIUM）。 */
    private static final String[] PREGNANCY_RELEVANT = {
            "下腹", "腹痛", "月经", "怀孕", "孕期", "白带", "胎", "出血", "分泌物"
    };

    private static final String[] SERIOUS_WORDS = {"肿瘤", "癌", "化疗", "放疗", "结节", "肿块"};
    private static final String[] HISTORY_RISK = {"高血压", "糖尿病", "心脏病", "肿瘤", "手术"};

    public TriageRuleEngine() {
    }

    // ==================== 对外入口 ====================

    public Result assess(String rawInput, Integer age, String ageBand, String gender,
                         String pregnant, String medicalHistory) {
        Result r = new Result();
        String normalized = normalize(rawInput);
        r.normalized = normalized;

        RedFlag red = detectRedFlag(normalized, age, ageBand);
        if (red.hit) {
            r.redFlag = true;
            r.redFlagPattern = red.pattern;
            r.redFlagReason = red.reason;
            r.tier = "RED_FLAG";
            r.confidence = 0.0;
            r.needExpert = "NOT_NEEDED";
            r.expertReason = "红旗症状优先急诊处置，不进入门诊预约";
            attachStructured(r, normalized, null, false, false, false, false);
            r.evidence.add(evidence("red_flag", red.pattern + "：" + red.reason));
            return r;
        }

        boolean preciseAge = age != null && age > 0;
        boolean preciseGender = gender != null && !gender.isEmpty();
        boolean bandUsed = !preciseAge && ageBand != null && !ageBand.isEmpty();
        boolean childAge = (preciseAge && age <= 14) || "CHILD".equalsIgnoreCase(ageBand);
        boolean elderAge = (preciseAge && age >= 65) || "ELDER".equalsIgnoreCase(ageBand);

        List<Match> matches = matchKeywords(normalized, gender);
        Map<String, Integer> deptScore = new LinkedHashMap<>();
        Map<String, Set<String>> deptKws = new LinkedHashMap<>();
        Map<String, Integer> strongCount = new LinkedHashMap<>();
        for (Match m : matches) {
            deptScore.merge(m.dept, m.weight, Integer::sum);
            deptKws.computeIfAbsent(m.dept, k -> new LinkedHashSet<>()).add(m.keyword);
            if (m.weight >= 2) {
                strongCount.merge(m.dept, 1, Integer::sum);
            }
        }

        // 儿科优先：精确年龄 ≤14 或年龄段为 CHILD 且提及儿童类关键词时加权
        if (childAge && deptScore.containsKey("儿科")) {
            deptScore.merge("儿科", 2, Integer::sum);
        }
        // 妇产科约束：男性不路由到下腹痛类妇科症状
        if ("MALE".equals(gender)) {
            Set<String> kws = deptKws.get("妇产科");
            if (kws != null && !kws.contains("月经") && !kws.contains("白带") && !kws.contains("怀孕") && !kws.contains("产检")) {
                deptScore.remove("妇产科");
                deptKws.remove("妇产科");
            }
        }

        List<DeptScore> ranked = rank(deptScore, deptKws, strongCount);
        r.departments = ranked;

        String dept = null;
        boolean fallback = false;
        if (!ranked.isEmpty() && ranked.get(0).score >= 2) {
            dept = ranked.get(0).name;
        }
        if (dept == null) {
            fallback = true;
            if (elderAge) {
                dept = "老年病科";
            } else if (containsAny(normalized, "乏力", "低热", "浑身", "没劲", "没力气")) {
                dept = "内科";
            }
        }
        r.deptName = dept;
        r.fallback = fallback;

        int top = ranked.isEmpty() ? 0 : ranked.get(0).score;
        int second = ranked.size() < 2 ? 0 : ranked.get(1).score;
        int kwCount = dept == null ? 0 : deptKws.containsKey(dept) ? deptKws.get(dept).size() : 0;

        boolean duration = hasDuration(normalized);
        boolean accompanying = hasAccompanying(normalized, matches.size());
        boolean topDominant = second == 0 || (top >= 2 * second);
        boolean ratioTight = second > 0 && top < 1.3 * second;
        boolean ambiguous = ranked.size() >= 2 && (top - second <= 1 || ratioTight);

        boolean femalePregnancyRisk = "FEMALE".equals(gender) && containsAny(normalized, PREGNANCY_RELEVANT)
                && (pregnant == null || pregnant.isEmpty() || "UNKNOWN".equals(pregnant));

        // ---------- 置信度 ----------
        double confidence = 0.45;
        int distinct = kwCount;
        confidence += 0.10 * Math.min(distinct, 3);
        if (!strongCount.isEmpty() && dept != null && strongCount.getOrDefault(dept, 0) > 0) {
            confidence += 0.12;
        }
        if (topDominant && top > 0) {
            confidence += 0.10;
        }
        if (duration) {
            confidence += 0.06;
        }
        if (accompanying) {
            confidence += 0.05;
        }
        if (preciseAge && preciseGender) {
            confidence += 0.05;
        }
        if (bandUsed) {
            confidence -= 0.05;
        }
        if (ratioTight) {
            confidence -= 0.10;
        }
        if (dept == null) {
            confidence -= 0.15;
        }
        if (normalized.length() < 6 || (distinct == 0 && containsAny(normalized, GENERIC_WORDS))) {
            confidence -= 0.10;
        }
        boolean historyMissing = medicalHistory == null || medicalHistory.trim().isEmpty();
        if (historyMissing) {
            confidence -= 0.05;
        }
        confidence = clamp(confidence);

        boolean highEligible = distinct >= 2
                && (duration || accompanying)
                && topDominant
                && !ambiguous
                && !fallback
                && preciseAge
                && preciseGender
                && !bandUsed
                && !femalePregnancyRisk
                && dept != null;
        if (!highEligible && confidence >= 0.90) {
            confidence = 0.89;
        }
        // 兜底科室、年龄段估算、孕产未明都属于“不足以直接挂号”的情况
        if ((fallback || bandUsed || femalePregnancyRisk) && confidence >= 0.90) {
            confidence = 0.89;
        }

        r.confidence = round(confidence);
        r.tier = r.confidence >= 0.90 ? "HIGH" : (r.confidence >= 0.70 ? "MEDIUM" : "LOW");

        // ---------- 证据 ----------
        if (dept != null && deptKws.containsKey(dept)) {
            for (String kw : deptKws.get(dept)) {
                r.evidence.add(evidence("keyword", "命中「" + kw + "」→ " + dept));
                r.keywords.add(kw);
            }
        }
        if (fallback) {
            r.evidence.add(evidence("fallback", "无明确系统归属，按年龄与泛症状兜底到「" + dept + "」"));
        }
        if (duration) {
            r.evidence.add(evidence("duration", "描述中包含持续时间，按病程进一步判断"));
        }
        if (accompanying) {
            r.evidence.add(evidence("accompanying", "描述中包含伴随症状或多症状并存"));
        }
        if (ranked.size() >= 2) {
            r.evidence.add(evidence("ranking", "首位科室得分 " + top + "，次位 "
                    + ranked.get(1).name + " " + second));
        }
        if (bandUsed) {
            r.evidence.add(evidence("age_band", "年龄为年龄段估算值，已下调置信度并封顶中档"));
        }
        if (historyMissing) {
            r.evidence.add(evidence("history", "未提供既往史，已小幅下调置信度，可补充后重新判断"));
        }
        if (femalePregnancyRisk) {
            r.evidence.add(evidence("pregnancy", "女性下腹/月经相关症状但怀孕状态未知，已封顶中档"));
        }
        if (r.tier.equals("LOW")) {
            r.followUps.add("不舒服有多久了？比如几小时、几天，还是更久？");
            r.followUps.add("除了主要不舒服，还有别的症状吗？例如发热、呕吐或疼痛加重。");
            r.followUps.add("以前有没有类似情况，或者正在长期吃什么药？");
        } else if (r.tier.equals("MEDIUM") && ambiguous && ranked.size() >= 2) {
            r.followUps.add("您的不适更接近「" + ranked.get(0).name + "」还是「"
                    + ranked.get(1).name + "」？可以说说最主要的感受。");
        }

        attachStructured(r, normalized, ranked, duration, accompanying, ambiguous, femalePregnancyRisk);

        // ---------- 专家判定 ----------
        Expert expert = judgeExpert(normalized, age, medicalHistory, ranked);
        r.expertScore = expert.score;
        r.needExpert = expert.level;
        r.expertReason = expert.reason;

        return r;
    }

    /** 供前端/服务层判断是否为“女性孕产相关且状态未知”的追问场景。 */
    public boolean needsPregnancyQuestion(String normalizedInput, Integer age, String ageBand,
                                          String gender, String pregnant) {
        if (!"FEMALE".equals(gender)) {
            return false;
        }
        if ("CHILD".equalsIgnoreCase(ageBand) || "ELDER".equalsIgnoreCase(ageBand)) {
            return false;
        }
        if (age != null && (age < 12 || age > 55)) {
            return false;
        }
        if (pregnant != null && !pregnant.isEmpty() && !"UNKNOWN".equals(pregnant)) {
            return false;
        }
        return containsAny(normalize(normalizedInput), PREGNANCY_RELEVANT);
    }

    // ==================== 红旗 ====================

    private static class RedFlag {
        boolean hit;
        String pattern;
        String reason;
    }

    private RedFlag detectRedFlag(String text, Integer age, String ageBand) {
        RedFlag r = new RedFlag();

        if ((hits(text, "胸痛") || hits(text, "胸口痛") || hits(text, "心口痛") || hits(text, "心口疼"))
                && (hits(text, "大汗") || hits(text, "压榨") || hits(text, "呼吸困难")
                || hits(text, "放射") || hits(text, "濒死") || hits(text, "剧烈"))) {
            return flag(r, "CHEST_PAIN", "胸痛伴大汗、压榨感或呼吸困难，需立即排查急性心血管事件");
        }
        if (hits(text, "剧烈胸痛")) {
            return flag(r, "CHEST_PAIN", "剧烈胸痛需立即排查急性心血管事件");
        }
        if (anyHits(text, "呼吸困难", "喘不上气", "窒息", "口唇发紫", "不能平卧")) {
            return flag(r, "DYSPNEA", "存在呼吸困难或窒息表现，需立即急诊处置");
        }
        if (anyHits(text, "嘴歪", "口角歪斜", "说话不清", "一侧肢体无力", "一侧肢体麻木",
                "半身不遂", "突然昏迷")) {
            return flag(r, "STROKE", "存在口角歪斜、言语不清或单侧肢体无力，需立即排查卒中");
        }
        if (anyHits(text, "呕血", "咯血", "大出血", "黑便", "大量血便")) {
            return flag(r, "BLEEDING", "存在呕血、咯血或大量出血表现，需立即急诊");
        }
        if (hits(text, "过敏") && anyHits(text, "喉头肿", "呼吸困难", "休克", "全身")) {
            return flag(r, "ANAPHYLAXIS", "过敏并出现喉头水肿、呼吸困难或休克表现，需立即急诊");
        }
        if ((hits(text, "怀孕") || hits(text, "孕妇") || hits(text, "孕期"))
                && anyHits(text, "阴道流血", "剧烈腹痛", "破水", "胎动减少")) {
            return flag(r, "PREGNANCY_RED", "孕期出现阴道流血、剧烈腹痛或破水，需立即产科急诊");
        }
        if (anyHits(text, "昏迷", "意识不清", "叫不醒", "抽搐", "惊厥")) {
            return flag(r, "CONSCIOUSNESS", "存在意识障碍、抽搐或惊厥，需立即急诊");
        }
        if (anyHits(text, "车祸", "高处坠落", "头部外伤", "摔到头")
                && anyHits(text, "昏迷", "呕吐", "出血")) {
            return flag(r, "TRAUMA", "外伤后出现昏迷、呕吐或出血，需立即急诊");
        }
        boolean child = (age != null && age > 0 && age <= 14) || "CHILD".equalsIgnoreCase(ageBand);
        if (child
                && anyHits(text, "高热", "高烧", "39度", "40度")
                && anyHits(text, "抽搐", "惊厥", "精神差", "精神很差", "精神不好", "没精神", "萎靡", "叫不醒")) {
            return flag(r, "CHILD_HIGH_FEVER", "儿童高热伴抽搐或精神状态差，需立即急诊");
        }
        if (anyHits(text, "自杀", "轻生", "不想活", "想死")) {
            return flag(r, "SELF_HARM", "出现自伤或轻生表达，请立即寻求身边人帮助并拨打心理援助热线");
        }
        return r;
    }

    private RedFlag flag(RedFlag r, String pattern, String reason) {
        r.hit = true;
        r.pattern = pattern;
        r.reason = reason;
        return r;
    }

    // ==================== 关键词与归一化 ====================

    private static class Match {
        String dept;
        String keyword;
        int weight;
        int start;
        int end;
    }

    private static class DeptScore {
        public String name;
        public int score;
        public List<String> keywords = new ArrayList<>();
        public int strong;
    }

    private List<Match> matchKeywords(String text, String gender) {
        List<Match> all = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> dept : DEPT_RULES.entrySet()) {
            if (EXCLUDED_DEPTS.contains(dept.getKey())) {
                continue;
            }
            for (Map.Entry<String, Integer> kw : dept.getValue().entrySet()) {
                int from = 0;
                while (from < text.length()) {
                    int idx = text.indexOf(kw.getKey(), from);
                    if (idx < 0) {
                        break;
                    }
                    from = idx + kw.getKey().length();
                    if (isNegated(text, idx)) {
                        continue;
                    }
                    // 妇科下腹痛仅在女性或未知性别时参与（男性已被上层过滤）
                    if ("下腹痛".equals(kw.getKey()) && "MALE".equals(gender)) {
                        continue;
                    }
                    Match m = new Match();
                    m.dept = dept.getKey();
                    m.keyword = kw.getKey();
                    m.weight = kw.getValue();
                    m.start = idx;
                    m.end = idx + kw.getKey().length();
                    all.add(m);
                }
            }
        }
        // 长关键词优先占用区间，避免「偏头痛」被「头痛」重复计数
        all.sort((a, b) -> Integer.compare(b.keyword.length(), a.keyword.length()));
        List<Match> kept = new ArrayList<>();
        for (Match m : all) {
            boolean overlap = false;
            for (Match k : kept) {
                if (m.start < k.end && m.end > k.start) {
                    overlap = true;
                    break;
                }
            }
            if (!overlap) {
                kept.add(m);
            }
        }
        return kept;
    }

    private List<DeptScore> rank(Map<String, Integer> scores, Map<String, Set<String>> kws,
                                 Map<String, Integer> strong) {
        List<DeptScore> list = new ArrayList<>();
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            DeptScore d = new DeptScore();
            d.name = e.getKey();
            d.score = e.getValue();
            d.keywords.addAll(kws.getOrDefault(e.getKey(), Collections.emptySet()));
            d.strong = strong.getOrDefault(e.getKey(), 0);
            list.add(d);
        }
        list.sort((a, b) -> b.score != a.score ? Integer.compare(b.score, a.score)
                : a.name.compareTo(b.name));
        return list;
    }

    /**
     * 否定词只在紧邻关键词时生效，避免「肚子不舒服，有点反酸」被误判为否定。
     * 允许否定词与关键词之间出现「明显/任何/怎么/太/很/再」等一个修饰词。
     */
    private boolean isNegated(String text, int index) {
        String prefix = text.substring(Math.max(0, index - 8), index);
        return prefix.matches(".*(没有|没|无|未|不|否认|无明显|未见)(明显|任何|怎么|太|很|再)?$");
    }

    private boolean hits(String text, String word) {
        int from = 0;
        while (from < text.length()) {
            int idx = text.indexOf(word, from);
            if (idx < 0) {
                return false;
            }
            from = idx + word.length();
            if (!isNegated(text, idx)) {
                return true;
            }
        }
        return false;
    }

    private boolean anyHits(String text, String... words) {
        for (String w : words) {
            if (hits(text, w)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String text, String... words) {
        for (String w : words) {
            if (text.contains(w)) {
                return true;
            }
        }
        return false;
    }

    /** 口语归一化：去空白、全角转半角、常见说法统一。 */
    public String normalize(String input) {
        if (input == null) {
            return "";
        }
        String text = input.trim().toLowerCase();
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c == '\u3000') {
                continue;
            }
            if (c >= '\uFF01' && c <= '\uFF5E') {
                sb.append((char) (c - 0xFEE0));
            } else {
                sb.append(c);
            }
        }
        text = sb.toString().replaceAll("[\\s\\r\\n\\t]+", "");
        // 长辈口语常带大量铺垫词，先去掉不影响判断的口头语，再统一说法
        String[] fillers = {"我跟你说", "我跟您说", "就是说", "怎么说呢", "你看啊", "你看", "那个",
                "嗯", "唉", "哎呀", "哎哟", "反正", "然后呢", "然后", "其实吧", "这个吧", "我这"};
        for (String filler : fillers) {
            text = text.replace(filler, "");
        }
        String[][] synonyms = {
                {"闹肚子", "腹泻"}, {"拉肚子", "腹泻"}, {"拉稀", "腹泻"}, {"肚子疼", "腹痛"},
                {"肚子痛", "腹痛"}, {"脑袋疼", "头痛"}, {"头疼", "头痛"}, {"后脑勺疼", "头痛"}, {"太阳穴疼", "头痛"},
                {"头昏", "头晕"}, {"脑袋迷糊", "头晕"}, {"脑子迷糊", "头晕"}, {"迷糊", "头晕"},
                {"晕乎乎", "头晕"}, {"天旋地转", "眩晕"}, {"心口疼", "心口痛"}, {"心口窝疼", "心口痛"},
                {"胸口发闷", "胸闷"}, {"胸口闷", "胸闷"}, {"憋闷", "胸闷"},
                {"心里发慌", "心慌"}, {"心里慌", "心慌"}, {"胃里难受", "胃不舒服"},
                {"胃里不舒服", "胃不舒服"}, {"吃不下饭", "没胃口"}, {"胃口不好", "没胃口"},
                {"打嗝", "嗳气"}, {"解大手费劲", "便秘"}, {"拉不出来", "便秘"},
                {"起夜多", "尿频"}, {"尿不出来", "排尿困难"}, {"看不清", "视物模糊"},
                {"瞅不清", "视物模糊"}, {"耳朵嗡嗡", "耳鸣"}, {"耳朵响", "耳鸣"},
                {"牙床肿", "牙龈"}, {"牙花子肿", "牙龈"}, {"痒痒", "瘙痒"}, {"起疙瘩", "皮疹"},
                {"睡不着觉", "失眠"}, {"睡不踏实", "失眠"}, {"腿发软", "乏力"},
                {"腿疼", "腿痛"}, {"腰疼", "腰痛"}, {"背疼", "背痛"}, {"关节疼", "关节痛"},
                {"膝盖疼", "膝盖"}, {"皮肤痒", "瘙痒"}, {"起疹子", "皮疹"},
                {"没劲儿", "没力气"}, {"气管炎", "支气管"}, {"老慢支", "支气管"},
                {"嗓子疼", "咽痛"}, {"嗓子发炎", "咽痛"}, {"鼻子不通气", "鼻塞"}, {"鼻子堵", "鼻塞"},
                {"有点发热", "发热"}, {"发高烧", "高热"}, {"高烧", "高热"},
                {"喘不上气来", "喘不上气"}, {"牙疼", "牙痛"}, {"不太舒服", "不舒服"},
                // 方言/口音常见说法：四川、重庆、粤语、河南、山东、东北
                {"脑壳昏", "头晕"}, {"脑壳晕", "头晕"}, {"脑壳疼", "头痛"}, {"脑壳痛", "头痛"},
                {"肚皮疼", "腹痛"}, {"肚皮痛", "腹痛"}, {"肚里疼", "腹痛"}, {"肚里痛", "腹痛"},
                {"屙肚子", "腹泻"}, {"屙稀", "腹泻"}, {"肚屙", "腹泻"}, {"屙肚", "腹泻"},
                {"心口子疼", "心口痛"}, {"打摆子", "发热"}, {"睡不戳", "失眠"}, {"睡唔着", "失眠"},
                {"眼睛雾", "视物模糊"}, {"睇唔清", "视物模糊"}, {"咳得凶", "咳嗽"},
                {"鼻子不通", "鼻塞"}, {"心口翳", "胸闷"}, {"耳仔嗡嗡", "耳鸣"},
                {"唔舒服", "不舒服"}, {"不得劲儿", "乏力"}, {"脑袋瓜子疼", "头痛"}
        };
        for (String[] pair : synonyms) {
            text = text.replace(pair[0], pair[1]);
        }
        return text;
    }

    private boolean hasDuration(String text) {
        if (text.matches(".*[0-9一二两三四五六七八九十百半几俩]+\\s*(天|日|小时|周|星期|个?月|年|分钟|宿).*")) {
            return true;
        }
        return containsAny(text, "多年", "半年", "好几天", "几天", "最近", "反复", "一直",
                "一段时间", "好久", "很久", "长期", "几天前", "上周", "昨晚", "今天",
                "好一阵子", "好些天", "半个月", "俩星期", "大半年", "前儿", "昨天夜里",
                "一宿", "老长时间", "好长时间", "这阵子", "好些日子");
    }

    private boolean hasAccompanying(String text, int matchCount) {
        if (containsAny(text, "而且", "还有", "伴", "同时", "并且", "另外", "以及", "加上")) {
            return true;
        }
        return matchCount >= 2;
    }

    // ==================== 专家判定 ====================

    private static class Expert {
        int score;
        String level;
        String reason;
    }

    private Expert judgeExpert(String text, Integer age, String medicalHistory, List<DeptScore> ranked) {
        Expert e = new Expert();
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (anyHits(text, "慢性", "反复", "长期", "多年", "控制不好", "老毛病", "经常", "好几年")) {
            score += 3;
            reasons.add("病程长、反复发作");
        }
        if (anyHits(text, "复查", "术后")) {
            score += 2;
            reasons.add("术后或需要复查评估");
        }
        if (anyHits(text, SERIOUS_WORDS)) {
            score += 2;
            reasons.add("涉及肿瘤、结节或肿块等需专科评估");
        }
        if (age != null) {
            if (age >= 75) {
                score += 2;
                reasons.add("高龄(≥75)");
            } else if (age >= 65) {
                score += 1;
                reasons.add("年龄65–74岁");
            } else if (age <= 3) {
                score += 2;
                reasons.add("婴幼儿(≤3岁)");
            }
        }
        if (medicalHistory != null && anyHits(medicalHistory, HISTORY_RISK)) {
            score += 2;
            reasons.add("既往有高血压/糖尿病/心脏病/肿瘤或手术史");
        }
        boolean multiDept = ranked.size() >= 2 && ranked.get(1).score >= 2;
        if (multiDept || text.length() >= 40) {
            score += 1;
            reasons.add(multiDept ? "多科室症状交叉，诊断需综合判断" : "描述复杂，涉及多个症状线索");
        }

        e.score = score;
        if (score >= 5) {
            e.level = "REQUIRED";
        } else if (score >= 3) {
            e.level = "RECOMMENDED";
        } else {
            e.level = "NOT_NEEDED";
        }
        if (reasons.isEmpty()) {
            e.reason = "症状单一、年龄与既往史无高风险因素，普通号即可";
        } else {
            e.reason = String.join("；", reasons);
            if ("NOT_NEEDED".equals(e.level)) {
                e.reason = e.reason + "；建议普通号，避免过度医疗";
            }
        }
        return e;
    }

    // ==================== 结果结构 ====================

    public static class Result {
        public String normalized;
        public boolean redFlag;
        public String redFlagPattern;
        public String redFlagReason;
        public String deptName;
        public double confidence;
        public String tier;
        public String needExpert = "NOT_NEEDED";
        public int expertScore;
        public String expertReason;
        public boolean fallback;
        public List<String> keywords = new ArrayList<>();
        public List<String> followUps = new ArrayList<>();
        public List<Map<String, Object>> evidence = new ArrayList<>();
        public List<DeptScore> departments = new ArrayList<>();
        public Map<String, Object> structured = new LinkedHashMap<>();
    }

    private Map<String, Object> evidence(String type, String detail) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        map.put("detail", detail);
        return map;
    }

    private void attachStructured(Result r, String normalized, List<DeptScore> ranked,
                                  boolean duration, boolean accompanying, boolean ambiguous,
                                  boolean pregnancyRisk) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("textLength", normalized.length());
        s.put("duration", duration);
        s.put("accompanying", accompanying);
        s.put("ambiguous", ambiguous);
        s.put("pregnancyRisk", pregnancyRisk);
        s.put("keywordCount", r.keywords.size());
        List<Map<String, Object>> depts = new ArrayList<>();
        if (ranked != null) {
            for (DeptScore d : ranked) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", d.name);
                m.put("score", d.score);
                m.put("keywords", d.keywords);
                depts.add(m);
            }
        }
        s.put("departments", depts);
        r.structured = s;
    }

    private double clamp(double v) {
        return Math.max(0.05, Math.min(0.97, v));
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static Map<String, Integer> keywords(List<String> strong, List<String> normal) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String s : strong) {
            map.put(s, 2);
        }
        for (String s : normal) {
            if (!map.containsKey(s)) {
                map.put(s, 1);
            }
        }
        return map;
    }

    private static List<String> strong(String... words) {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, words);
        return list;
    }

    private static List<String> normal(String... words) {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, words);
        return list;
    }
}
