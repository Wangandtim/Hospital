import random
import string
from datetime import datetime, timedelta

random.seed(42)

DEPARTMENTS = [
    '心血管内科', '呼吸内科', '消化内科', '神经内科', '骨科', '普外科',
    '妇产科', '儿科', '眼科', '口腔科', '皮肤科', '急诊科', '中医科',
    '内分泌科', '肾内科', '风湿免疫科', '肿瘤科', '血液科', '感染科',
    '神经外科', '胸外科', '泌尿外科', '肝胆外科', '耳鼻喉科', '麻醉科',
    '康复医学科', '医学检验科', '医学影像科', '病理科', '药剂科'
]

DEPT_INFO = {
    '心血管内科': ('诊治心脏及血管疾病', '冠心病、高血压、心律失常、心力衰竭'),
    '呼吸内科': ('诊治呼吸系统疾病', '肺炎、哮喘、慢性阻塞性肺疾病、肺癌'),
    '消化内科': ('诊治消化系统疾病', '胃炎、胃溃疡、肝炎、胰腺炎'),
    '神经内科': ('诊治神经系统疾病', '脑血管病、帕金森病、癫痫、头痛'),
    '骨科': ('诊治骨骼肌肉系统疾病', '骨折、关节置换、脊柱疾病、运动损伤'),
    '普外科': ('诊治普通外科疾病', '阑尾炎、胆囊炎、甲状腺疾病、乳腺疾病'),
    '妇产科': ('诊治妇科及产科疾病', '妇科肿瘤、不孕不育、妊娠管理'),
    '儿科': ('诊治儿童疾病', '小儿呼吸道感染、小儿腹泻、小儿发育'),
    '眼科': ('诊治眼部疾病', '白内障、青光眼、近视、眼底病'),
    '口腔科': ('诊治口腔疾病', '牙周病、龋齿、口腔肿瘤、正畸'),
    '皮肤科': ('诊治皮肤疾病', '湿疹、银屑病、痤疮、皮肤肿瘤'),
    '急诊科': ('处理急症和创伤', '外伤急救、心脑血管急症、中毒'),
    '中医科': ('中医诊疗', '中医内科、针灸推拿、中医调理'),
    '内分泌科': ('诊治内分泌疾病', '糖尿病、甲状腺疾病、骨质疏松'),
    '肾内科': ('诊治肾脏疾病', '肾炎、肾病综合征、尿毒症'),
    '风湿免疫科': ('诊治风湿免疫疾病', '类风湿关节炎、系统性红斑狼疮'),
    '肿瘤科': ('诊治肿瘤疾病', '肿瘤化疗、放疗、靶向治疗'),
    '血液科': ('诊治血液系统疾病', '白血病、贫血、淋巴瘤'),
    '感染科': ('诊治感染性疾病', '病毒性肝炎、艾滋病、结核病'),
    '神经外科': ('诊治神经外科疾病', '脑肿瘤、脑出血、脊髓损伤'),
    '胸外科': ('诊治胸部疾病', '肺癌、食管癌、纵隔肿瘤'),
    '泌尿外科': ('诊治泌尿系统疾病', '肾结石、前列腺疾病、膀胱癌'),
    '肝胆外科': ('诊治肝胆胰疾病', '肝癌、胆结石、胰腺疾病'),
    '耳鼻喉科': ('诊治耳鼻喉疾病', '中耳炎、鼻窦炎、喉癌'),
    '麻醉科': ('临床麻醉及疼痛治疗', '手术麻醉、疼痛管理'),
    '康复医学科': ('康复治疗', '神经康复、骨科康复、心肺康复'),
    '医学检验科': ('医学检验', '血常规、生化检查、微生物检验'),
    '医学影像科': ('医学影像诊断', 'X光、CT、MRI、超声'),
    '病理科': ('病理诊断', '病理活检、细胞学检查'),
    '药剂科': ('药学服务', '药品管理、临床药学')
}

FIRST_NAMES = ['张', '李', '王', '刘', '陈', '杨', '赵', '黄', '周', '吴',
               '徐', '孙', '马', '朱', '胡', '郭', '何', '林', '罗', '高',
               '郑', '梁', '谢', '宋', '唐', '许', '韩', '冯', '邓', '曹']

LAST_NAMES = ['伟', '强', '磊', '敏', '静', '芳', '丽', '杰', '涛', '军',
              '明', '华', '亮', '勇', '文', '燕', '霞', '峰', '刚', '平',
              '辉', '敏', '娜', '鹏', '鑫', '婷', '宇', '洋', '浩', '雨']

TITLES = ['主任医师', '副主任医师', '主治医师', '住院医师']
TITLE_WEIGHTS = [0.2, 0.3, 0.4, 0.1]

EXPERTISE_TEMPLATES = {
    '心血管内科': ['冠心病介入治疗', '高血压诊治', '心律失常射频消融', '心力衰竭管理'],
    '呼吸内科': ['肺部感染诊治', '哮喘个体化治疗', 'COPD综合管理', '肺癌早期筛查'],
    '消化内科': ['胃镜肠镜检查', '消化性溃疡治疗', '肝病诊治', '炎症性肠病'],
    '神经内科': ['脑血管病诊治', '帕金森病管理', '癫痫诊疗', '头痛眩晕'],
    '骨科': ['关节置换手术', '脊柱微创手术', '运动损伤修复', '骨折固定'],
    '普外科': ['腹腔镜手术', '甲状腺手术', '乳腺疾病诊治', '胃肠外科'],
    '妇产科': ['妇科肿瘤诊治', '不孕不育治疗', '高危妊娠管理', '妇科微创手术'],
    '儿科': ['小儿呼吸疾病', '小儿消化疾病', '新生儿重症', '儿童保健'],
    '眼科': ['白内障手术', '青光眼诊治', '屈光不正矫正', '眼底病治疗'],
    '口腔科': ['种植牙修复', '正畸治疗', '牙周病诊治', '口腔颌面外科'],
    '皮肤科': ['湿疹银屑病治疗', '皮肤肿瘤诊治', '痤疮治疗', '过敏性皮肤病'],
    '急诊科': ['创伤急救', '心肺复苏', '中毒救治', '多器官功能衰竭'],
    '中医科': ['中医内科调理', '针灸推拿', '慢性病中医治疗', '养生保健'],
    '内分泌科': ['糖尿病个体化治疗', '甲状腺疾病诊治', '骨质疏松防治', '代谢综合征'],
    '肾内科': ['慢性肾病管理', '血液透析', '肾移植术后', '肾小球疾病'],
    '风湿免疫科': ['类风湿关节炎诊治', '系统性红斑狼疮', '强直性脊柱炎', '干燥综合征'],
    '肿瘤科': ['肿瘤化疗', '靶向治疗', '免疫治疗', '肿瘤综合治疗'],
    '血液科': ['白血病诊治', '淋巴瘤治疗', '贫血诊治', '干细胞移植'],
    '感染科': ['病毒性肝炎诊治', '艾滋病管理', '结核病防治', '发热待查'],
    '神经外科': ['脑肿瘤手术', '脑出血手术', '椎管肿瘤', '颅脑外伤'],
    '胸外科': ['肺癌手术', '食管癌手术', '纵隔肿瘤', '胸腔镜手术'],
    '泌尿外科': ['前列腺手术', '肾结石治疗', '膀胱癌诊治', '肾移植'],
    '肝胆外科': ['肝癌手术', '胆囊切除术', '胰腺疾病', '肝硬化'],
    '耳鼻喉科': ['耳科疾病诊治', '鼻科疾病诊治', '咽喉疾病', '头颈外科'],
    '麻醉科': ['全身麻醉', '椎管内麻醉', '疼痛管理', '重症监护'],
    '康复医学科': ['神经康复', '骨科康复', '心肺康复', '康复评定'],
    '医学检验科': ['临床生化检验', '微生物检验', '血液学检验', '免疫检验'],
    '医学影像科': ['CT诊断', 'MRI诊断', '超声诊断', '介入放射'],
    '病理科': ['病理诊断', '细胞学检查', '分子病理', '冰冻切片'],
    '药剂科': ['临床药学', '药物咨询', '合理用药', '药品管理']
}

EDUCATION_TEMPLATES = [
    '毕业于{}医科大学，医学博士学位',
    '毕业于{}医科大学，医学硕士学位',
    '毕业于{}医科大学，学士学位',
    '{}医科大学博士后'
]

UNIVERSITIES = ['北京', '上海', '复旦大学', '中山大学', '浙江大学', '华中科技大学', '四川大学', '西安交通大学']

HOSPITALS = [
    {'name': '西安交通大学第一附属医院', 'address': '陕西省西安市雁塔区雁塔西路277号', 'phone': '029-85323201', 'level': '三级甲等', 'lat': 34.2277, 'lng': 108.9294, 'province': '陕西省', 'city': '西安市', 'district': '雁塔区'},
    {'name': '空军军医大学西京医院', 'address': '陕西省西安市新城区长乐西路127号', 'phone': '029-84775500', 'level': '三级甲等', 'lat': 34.2717, 'lng': 108.9607, 'province': '陕西省', 'city': '西安市', 'district': '新城区'},
    {'name': '陕西省人民医院', 'address': '陕西省西安市碑林区友谊西路256号', 'phone': '029-85276000', 'level': '三级甲等', 'lat': 34.2221, 'lng': 108.9468, 'province': '陕西省', 'city': '西安市', 'district': '碑林区'},
    {'name': '西安医学院第二附属医院', 'address': '陕西省西安市新城区尚勤路3号', 'phone': '029-87679360', 'level': '三级甲等', 'lat': 34.2662, 'lng': 108.9655, 'province': '陕西省', 'city': '西安市', 'district': '新城区'},
    {'name': '西安市中心医院', 'address': '陕西省西安市新城区西五路161号', 'phone': '029-87218536', 'level': '三级甲等', 'lat': 34.2723, 'lng': 108.9520, 'province': '陕西省', 'city': '西安市', 'district': '新城区'},
    {'name': '西安市第一医院', 'address': '陕西省西安市碑林区南大街2号', 'phone': '029-87252000', 'level': '三级甲等', 'lat': 34.2333, 'lng': 108.9473, 'province': '陕西省', 'city': '西安市', 'district': '碑林区'},
    {'name': '西安市第三医院', 'address': '陕西省西安市未央区凤城三路东段10号', 'phone': '029-61810000', 'level': '三级甲等', 'lat': 34.3052, 'lng': 108.9503, 'province': '陕西省', 'city': '西安市', 'district': '未央区'},
    {'name': '西北妇女儿童医院', 'address': '陕西省西安市雁塔区雁引路1号', 'phone': '029-89550000', 'level': '三级甲等', 'lat': 34.2168, 'lng': 108.9632, 'province': '陕西省', 'city': '西安市', 'district': '雁塔区'},
    {'name': '西安市第四医院', 'address': '陕西省西安市新城区解放路2号', 'phone': '029-87480734', 'level': '三级甲等', 'lat': 34.2508, 'lng': 108.9602, 'province': '陕西省', 'city': '西安市', 'district': '新城区'},
    {'name': '西安唐都医院', 'address': '陕西省西安市灞桥区新寺路1号', 'phone': '029-84777777', 'level': '三级甲等', 'lat': 34.2638, 'lng': 109.0678, 'province': '陕西省', 'city': '西安市', 'district': '灞桥区'},
    {'name': '西安高新医院', 'address': '陕西省西安市高新区团结南路16号', 'phone': '029-88855555', 'level': '三级甲等', 'lat': 34.2001, 'lng': 108.8897, 'province': '陕西省', 'city': '西安市', 'district': '高新区'},
    {'name': '西安国际医学中心医院', 'address': '陕西省西安市高新区西太大道777号', 'phone': '029-68301111', 'level': '三级甲等', 'lat': 34.1472, 'lng': 108.8256, 'province': '陕西省', 'city': '西安市', 'district': '高新区'},
    {'name': '四川大学华西医院', 'address': '四川省成都市武侯区国学巷37号', 'phone': '028-85422114', 'level': '三级甲等', 'lat': 30.6550, 'lng': 104.0388, 'province': '四川省', 'city': '成都市', 'district': '武侯区'},
    {'name': '四川省人民医院', 'address': '四川省成都市青羊区一环路西二段32号', 'phone': '028-87393666', 'level': '三级甲等', 'lat': 30.6616, 'lng': 104.0264, 'province': '四川省', 'city': '成都市', 'district': '青羊区'},
    {'name': '成都中医药大学附属医院', 'address': '四川省成都市金牛区十二桥路39号', 'phone': '028-87769900', 'level': '三级甲等', 'lat': 30.6727, 'lng': 104.0178, 'province': '四川省', 'city': '成都市', 'district': '金牛区'},
    {'name': '成都市第三人民医院', 'address': '四川省成都市青羊区青龙街82号', 'phone': '028-86649476', 'level': '三级甲等', 'lat': 30.6776, 'lng': 104.0386, 'province': '四川省', 'city': '成都市', 'district': '青羊区'},
    {'name': '成都军区总医院', 'address': '四川省成都市金牛区蓉都大道天回路270号', 'phone': '028-86570311', 'level': '三级甲等', 'lat': 30.7238, 'lng': 104.0871, 'province': '四川省', 'city': '成都市', 'district': '金牛区'},
    {'name': '成都市第二人民医院', 'address': '四川省成都市锦江区庆云南街10号', 'phone': '028-86742246', 'level': '三级甲等', 'lat': 30.6594, 'lng': 104.0666, 'province': '四川省', 'city': '成都市', 'district': '锦江区'},
    {'name': '四川省肿瘤医院', 'address': '四川省成都市成华区二环路东二段29号', 'phone': '028-84522120', 'level': '三级甲等', 'lat': 30.6752, 'lng': 104.0918, 'province': '四川省', 'city': '成都市', 'district': '成华区'},
    {'name': '成都大学附属医院', 'address': '四川省成都市金牛区二环路北二段29号', 'phone': '028-83572222', 'level': '三级甲等', 'lat': 30.6947, 'lng': 104.0471, 'province': '四川省', 'city': '成都市', 'district': '金牛区'},
    {'name': '四川省妇幼保健院', 'address': '四川省成都市武侯区沙堰西二街290号', 'phone': '028-65978333', 'level': '三级甲等', 'lat': 30.6328, 'lng': 103.9971, 'province': '四川省', 'city': '成都市', 'district': '武侯区'},
    {'name': '北京大学第一医院', 'address': '北京市西城区西什库大街8号', 'phone': '010-83572211', 'level': '三级甲等', 'lat': 39.9207, 'lng': 116.3757, 'province': '北京市', 'city': '北京市', 'district': '西城区'},
    {'name': '北京协和医院', 'address': '北京市东城区东单北大街69号', 'phone': '010-65296114', 'level': '三级甲等', 'lat': 39.9138, 'lng': 116.4021, 'province': '北京市', 'city': '北京市', 'district': '东城区'},
    {'name': '中国人民解放军总医院', 'address': '北京市海淀区复兴路28号', 'phone': '010-66887329', 'level': '三级甲等', 'lat': 39.9083, 'lng': 116.2846, 'province': '北京市', 'city': '北京市', 'district': '海淀区'},
    {'name': '首都医科大学附属北京天坛医院', 'address': '北京市丰台区南四环西路119号', 'phone': '010-59976611', 'level': '三级甲等', 'lat': 39.8272, 'lng': 116.2896, 'province': '北京市', 'city': '北京市', 'district': '丰台区'},
    {'name': '首都医科大学附属北京友谊医院', 'address': '北京市西城区永安路95号', 'phone': '010-63138114', 'level': '三级甲等', 'lat': 39.8938, 'lng': 116.3940, 'province': '北京市', 'city': '北京市', 'district': '西城区'},
    {'name': '复旦大学附属中山医院', 'address': '上海市徐汇区枫林路180号', 'phone': '021-64041990', 'level': '三级甲等', 'lat': 31.2192, 'lng': 121.4512, 'province': '上海市', 'city': '上海市', 'district': '徐汇区'},
    {'name': '上海交通大学医学院附属瑞金医院', 'address': '上海市黄浦区瑞金二路197号', 'phone': '021-64370045', 'level': '三级甲等', 'lat': 31.2183, 'lng': 121.4699, 'province': '上海市', 'city': '上海市', 'district': '黄浦区'},
    {'name': '复旦大学附属华山医院', 'address': '上海市静安区乌鲁木齐中路12号', 'phone': '021-52889999', 'level': '三级甲等', 'lat': 31.2317, 'lng': 121.4607, 'province': '上海市', 'city': '上海市', 'district': '静安区'},
    {'name': '上海交通大学医学院附属仁济医院', 'address': '上海市浦东新区东方路1630号', 'phone': '021-58752345', 'level': '三级甲等', 'lat': 31.2386, 'lng': 121.5214, 'province': '上海市', 'city': '上海市', 'district': '浦东新区'},
    {'name': '中山大学附属第一医院', 'address': '广东省广州市越秀区中山二路58号', 'phone': '020-87755766', 'level': '三级甲等', 'lat': 23.1303, 'lng': 113.3047, 'province': '广东省', 'city': '广州市', 'district': '越秀区'},
    {'name': '南方医科大学南方医院', 'address': '广东省广州市白云区广州大道北1838号', 'phone': '020-61641114', 'level': '三级甲等', 'lat': 23.2141, 'lng': 113.3268, 'province': '广东省', 'city': '广州市', 'district': '白云区'},
    {'name': '广东省人民医院', 'address': '广东省广州市越秀区中山二路106号', 'phone': '020-83827812', 'level': '三级甲等', 'lat': 23.1323, 'lng': 113.3024, 'province': '广东省', 'city': '广州市', 'district': '越秀区'},
    {'name': '广州市第一人民医院', 'address': '广东省广州市越秀区盘福路1号', 'phone': '020-81048888', 'level': '三级甲等', 'lat': 23.1363, 'lng': 113.2577, 'province': '广东省', 'city': '广州市', 'district': '越秀区'},
    {'name': '浙江大学医学院附属第一医院', 'address': '浙江省杭州市上城区庆春路79号', 'phone': '0571-87236114', 'level': '三级甲等', 'lat': 30.2671, 'lng': 120.1553, 'province': '浙江省', 'city': '杭州市', 'district': '上城区'},
    {'name': '浙江大学医学院附属第二医院', 'address': '浙江省杭州市上城区解放路88号', 'phone': '0571-87783777', 'level': '三级甲等', 'lat': 30.2628, 'lng': 120.1592, 'province': '浙江省', 'city': '杭州市', 'district': '上城区'},
    {'name': '浙江省人民医院', 'address': '浙江省杭州市下城区上塘路158号', 'phone': '0571-85239988', 'level': '三级甲等', 'lat': 30.2924, 'lng': 120.1561, 'province': '浙江省', 'city': '杭州市', 'district': '下城区'},
    {'name': '江苏省人民医院', 'address': '江苏省南京市鼓楼区广州路300号', 'phone': '025-83718836', 'level': '三级甲等', 'lat': 32.0655, 'lng': 118.7868, 'province': '江苏省', 'city': '南京市', 'district': '鼓楼区'},
    {'name': '南京鼓楼医院', 'address': '江苏省南京市鼓楼区中山路321号', 'phone': '025-83105555', 'level': '三级甲等', 'lat': 32.0628, 'lng': 118.7913, 'province': '江苏省', 'city': '南京市', 'district': '鼓楼区'},
    {'name': '华中科技大学同济医学院附属同济医院', 'address': '湖北省武汉市硚口区解放大道1095号', 'phone': '027-83662688', 'level': '三级甲等', 'lat': 30.5728, 'lng': 114.2738, 'province': '湖北省', 'city': '武汉市', 'district': '硚口区'},
    {'name': '武汉大学人民医院', 'address': '湖北省武汉市武昌区张之洞路95号', 'phone': '027-88041911', 'level': '三级甲等', 'lat': 30.5567, 'lng': 114.3225, 'province': '湖北省', 'city': '武汉市', 'district': '武昌区'},
    {'name': '中南大学湘雅医院', 'address': '湖南省长沙市开福区湘雅路87号', 'phone': '0731-84328888', 'level': '三级甲等', 'lat': 28.2281, 'lng': 112.9380, 'province': '湖南省', 'city': '长沙市', 'district': '开福区'},
    {'name': '湖南省人民医院', 'address': '湖南省长沙市芙蓉区解放西路61号', 'phone': '0731-82278088', 'level': '三级甲等', 'lat': 28.2022, 'lng': 112.9954, 'province': '湖南省', 'city': '长沙市', 'district': '芙蓉区'},
    {'name': '山东大学齐鲁医院', 'address': '山东省济南市历下区文化西路107号', 'phone': '0531-82169114', 'level': '三级甲等', 'lat': 36.6670, 'lng': 116.9975, 'province': '山东省', 'city': '济南市', 'district': '历下区'},
    {'name': '山东省立医院', 'address': '山东省济南市槐荫区经五路324号', 'phone': '0531-87938911', 'level': '三级甲等', 'lat': 36.6559, 'lng': 116.9769, 'province': '山东省', 'city': '济南市', 'district': '槐荫区'},
    {'name': '郑州大学第一附属医院', 'address': '河南省郑州市二七区建设东路1号', 'phone': '0371-66278510', 'level': '三级甲等', 'lat': 34.7493, 'lng': 113.6586, 'province': '河南省', 'city': '郑州市', 'district': '二七区'},
    {'name': '河南省人民医院', 'address': '河南省郑州市金水区纬五路7号', 'phone': '0371-65580014', 'level': '三级甲等', 'lat': 34.7683, 'lng': 113.6660, 'province': '河南省', 'city': '郑州市', 'district': '金水区'},
    {'name': '山西省人民医院', 'address': '山西省太原市迎泽区解放南路85号', 'phone': '0351-4960000', 'level': '三级甲等', 'lat': 37.8716, 'lng': 112.5492, 'province': '山西省', 'city': '太原市', 'district': '迎泽区'},
    {'name': '河北医科大学第二医院', 'address': '河北省石家庄市新华区和平西路215号', 'phone': '0311-87046901', 'level': '三级甲等', 'lat': 38.0467, 'lng': 114.4844, 'province': '河北省', 'city': '石家庄市', 'district': '新华区'},
    {'name': '安徽省立医院', 'address': '安徽省合肥市庐阳区庐江路17号', 'phone': '0551-62283120', 'level': '三级甲等', 'lat': 31.8609, 'lng': 117.2273, 'province': '安徽省', 'city': '合肥市', 'district': '庐阳区'},
    {'name': '福建医科大学附属协和医院', 'address': '福建省福州市鼓楼区新权路29号', 'phone': '0591-83357199', 'level': '三级甲等', 'lat': 26.0710, 'lng': 119.3034, 'province': '福建省', 'city': '福州市', 'district': '鼓楼区'},
    {'name': '南昌大学第一附属医院', 'address': '江西省南昌市东湖区永外正街17号', 'phone': '0791-88692750', 'level': '三级甲等', 'lat': 28.6824, 'lng': 115.8978, 'province': '江西省', 'city': '南昌市', 'district': '东湖区'},
    {'name': '广西医科大学第一附属医院', 'address': '广西壮族自治区南宁市青秀区双拥路6号', 'phone': '0771-5359339', 'level': '三级甲等', 'lat': 22.8258, 'lng': 108.3188, 'province': '广西壮族自治区', 'city': '南宁市', 'district': '青秀区'},
    {'name': '云南省第一人民医院', 'address': '云南省昆明市五华区金碧路157号', 'phone': '0871-63638929', 'level': '三级甲等', 'lat': 25.0399, 'lng': 102.7186, 'province': '云南省', 'city': '昆明市', 'district': '五华区'},
    {'name': '贵州省人民医院', 'address': '贵州省贵阳市南明区市南路81号', 'phone': '0851-85922979', 'level': '三级甲等', 'lat': 26.5888, 'lng': 106.7058, 'province': '贵州省', 'city': '贵阳市', 'district': '南明区'},
    {'name': '甘肃省人民医院', 'address': '甘肃省兰州市城关区东岗西路204号', 'phone': '0931-8281763', 'level': '三级甲等', 'lat': 36.0601, 'lng': 103.8464, 'province': '甘肃省', 'city': '兰州市', 'district': '城关区'},
    {'name': '新疆医科大学第一附属医院', 'address': '新疆维吾尔自治区乌鲁木齐市新市区鲤鱼山南路137号', 'phone': '0991-4362705', 'level': '三级甲等', 'lat': 43.8020, 'lng': 87.6071, 'province': '新疆维吾尔自治区', 'city': '乌鲁木齐市', 'district': '新市区'}
]

def generate_hospital_sql():
    sql = []
    for i, hospital in enumerate(HOSPITALS, 1):
        hospital_id = f"H{i:03d}"
        description = f"{hospital['name']}是{hospital['level']}综合性医院，位于{hospital['address']}，为当地居民提供优质医疗服务。"
        flagship_depts = random.sample([d for d in DEPARTMENTS[:10]], 3)
        flagship_dept = ','.join(flagship_depts)
        create_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
        sql.append(f"""INSERT INTO hospital (hospital_id, hospital_name, address, phone, level, description, flagship_dept, website_url, latitude, longitude, status, create_time, province, city, district) VALUES
('{hospital_id}', '{hospital['name']}', '{hospital['address']}', '{hospital['phone']}', '{hospital['level']}', '{description}', '{flagship_dept}', '', {hospital['lat']}, {hospital['lng']}, 1, '{create_time}', '{hospital['province']}', '{hospital['city']}', '{hospital['district']}');""")
    return sql

def precompute_hospital_depts():
    hospital_depts = []
    for i, hospital in enumerate(HOSPITALS, 1):
        hospital_id = f"H{i:03d}"
        num_depts = random.randint(8, 12)
        depts = random.sample(DEPARTMENTS, num_depts)
        dept_info = []
        for j, dept_name in enumerate(depts, 1):
            dept_id = f"{hospital_id}_DEPT{j:03d}"
            num_doctors = random.randint(2, 5)
            dept_info.append((dept_id, dept_name, num_doctors))
        hospital_depts.append((hospital_id, dept_info))
    return hospital_depts

def generate_department_sql(hospital_depts):
    sql = []
    for hospital_id, dept_info in hospital_depts:
        flagship_indices = random.sample(range(len(dept_info)), min(3, len(dept_info)))
        for idx, (dept_id, dept_name, _) in enumerate(dept_info):
            is_flagship = 1 if idx in flagship_indices else 0
            info = DEPT_INFO.get(dept_name, ('', ''))
            create_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            
            sql.append(f"""INSERT INTO department (id, hospital_id, name, description, diseases, service_time, is_flagship, create_time) VALUES
('{dept_id}', '{hospital_id}', '{dept_name}', '{info[0]}', '{info[1]}', '周一至周日 8:00-17:30', {is_flagship}, '{create_time}');""")
    return sql

def generate_doctor_sql(hospital_depts):
    sql = []
    doctor_counter = 1
    
    for hospital_id, dept_info in hospital_depts:
        for dept_id, dept_name, num_doctors in dept_info:
            for _ in range(num_doctors):
                doctor_id = f"DOC{doctor_counter:05d}"
                doctor_counter += 1
                
                gender = random.choice(['男', '女'])
                title = random.choices(TITLES, weights=TITLE_WEIGHTS)[0]
                title_fee_map = {'主任医师': 100, '副主任医师': 70, '主治医师': 50, '住院医师': 30}
                fee = title_fee_map[title]
                
                first_name = random.choice(FIRST_NAMES)
                last_name = random.choice(LAST_NAMES)
                name = first_name + last_name
                
                expertise_list = EXPERTISE_TEMPLATES.get(dept_name, ['临床诊疗'])
                expertise = ', '.join(random.sample(expertise_list, min(2, len(expertise_list))))
                
                education = EDUCATION_TEMPLATES[random.randint(0, 3)].format(random.choice(UNIVERSITIES))
                
                phone = '1' + ''.join(random.choices(string.digits, k=10))
                username = f"doctor_{hospital_id}_{doctor_counter-1}"
                password = '123456'
                
                experience = f"从事{dept_name}临床工作{random.randint(5, 30)}年，具有丰富的临床经验。"
                moral = "医德高尚，耐心细致，深受患者好评。"
                
                sql.append(f"""INSERT INTO doctor (doctor_id, hospital_id, name, gender, dept_id, title, expertise, phone, education, moral, experience, status, consultation_fee, username, password) VALUES
('{doctor_id}', '{hospital_id}', '{name}', '{gender}', '{dept_id}', '{title}', '{expertise}', '{phone}', '{education}', '{moral}', '{experience}', 1, {fee}, '{username}', '{password}');""")
    return sql, doctor_counter - 1

def generate_schedule_sql(hospital_depts, total_doctors):
    sql = []
    doctor_counter = 1
    today = datetime.now().date()
    
    morning_slots = [
        ('am1', '09:00-09:20'),
        ('am2', '09:20-09:40'),
        ('am3', '09:40-10:00')
    ]
    
    afternoon_slots = [
        ('pm1', '14:00-14:20'),
        ('pm2', '14:20-14:40'),
        ('pm3', '14:40-15:00')
    ]
    
    all_slots = morning_slots + afternoon_slots
    
    for hospital_id, dept_info in hospital_depts:
        for _, _, num_doctors in dept_info:
            for _ in range(num_doctors):
                doctor_id = f"DOC{doctor_counter:05d}"
                doctor_counter += 1
                
                for day_offset in range(1, 8):
                    work_date = today + timedelta(days=day_offset)
                    date_str = work_date.strftime('%Y%m%d')
                    
                    for slot_code, time_slot in all_slots:
                        schedule_id = f"SCH_{doctor_id}_{date_str}_{slot_code}"
                        
                        sql.append(f"""INSERT INTO schedule (schedule_id, hospital_id, doctor_id, work_date, time_slot, max_patients, booked_count, status, version) VALUES
('{schedule_id}', '{hospital_id}', '{doctor_id}', '{work_date}', '{time_slot}', 5, 0, 1, 0);""")
    return sql

def main():
    print("正在预计算医院科室信息...")
    hospital_depts = precompute_hospital_depts()
    
    print("正在生成医院数据...")
    hospital_sql = generate_hospital_sql()
    print(f"生成 {len(hospital_sql)} 家医院")
    
    print("正在生成科室数据...")
    dept_sql = generate_department_sql(hospital_depts)
    print(f"生成 {len(dept_sql)} 个科室")
    
    print("正在生成医生数据...")
    doctor_sql, total_doctors = generate_doctor_sql(hospital_depts)
    print(f"生成 {len(doctor_sql)} 名医生")
    
    print("正在生成排班数据...")
    schedule_sql = generate_schedule_sql(hospital_depts, total_doctors)
    print(f"生成 {len(schedule_sql)} 条排班")
    
    all_sql = ['-- 医院数据'] + hospital_sql + ['\n-- 科室数据'] + dept_sql + ['\n-- 医生数据'] + doctor_sql + ['\n-- 排班数据'] + schedule_sql
    
    with open('data_new.sql', 'w', encoding='utf-8') as f:
        f.write('\n'.join(all_sql))
    
    print("\n数据生成完成！输出文件: data_new.sql")
    print(f"总计：{len(hospital_sql)}家医院, {len(dept_sql)}个科室, {len(doctor_sql)}名医生, {len(schedule_sql)}条排班")

if __name__ == '__main__':
    main()