package com.hospital.config;

import com.hospital.entity.*;
import com.hospital.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_HOSPITAL_ID = "H001";

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ScheduleRules scheduleRules;

    /** 医生编号全局顺延，避免同一位医生出现 H001_DOC001 与 DOC001 两种写法 */
    private int doctorSeq = 0;

    private String nextDoctorId() {
        return String.format("DOC%03d", ++doctorSeq);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("========================================");
        System.out.println("开始检查医院管理系统数据...");
        System.out.println("========================================");

        // 只在医院数据为空时才初始化
        if (hospitalRepository.count() == 0) {
            System.out.println("数据库为空，开始初始化数据...");
            
            initHospitals();
            initAdmins();
            initQionglaiHospital();
            initXianJiaodaHospital();
            initXijingHospital();
            initShanxiRenminHospital();
            initHuaxiHospital();

            if (patientRepository.count() == 0) {
                initPatients();
            }
            
            System.out.println("========================================");
            System.out.println("医院管理系统数据初始化完成！");
            System.out.println("========================================");
        } else {
            System.out.println("数据已存在，跳过初始化");
            System.out.println("医院数量: " + hospitalRepository.count());
            System.out.println("医生数量: " + doctorRepository.count());
            System.out.println("科室数量: " + departmentRepository.count());
            System.out.println("排班数量: " + scheduleRepository.count());
            
            System.out.println("========================================");
            System.out.println("医院管理系统数据检查完成！");
            System.out.println("========================================");
        }
    }

    private void initHospitals() {
        Hospital hospital1 = new Hospital();
        hospital1.setHospitalId(DEFAULT_HOSPITAL_ID);
        hospital1.setHospitalName("邛崃市医疗中心医院");
        hospital1.setAddress("四川省成都市邛崃市文君街道文昌街70号");
        hospital1.setPhone("028-88791120");
        hospital1.setLevel("二级甲等");
        hospital1.setDescription("邛崃市医疗中心医院是当地规模最大、技术力量最雄厚的综合性医疗机构。医院拥有先进的医疗设备和优秀的医护团队，致力于为患者提供高质量、安全、便捷的医疗服务。");
        hospital1.setFlagshipDept("急诊科、心血管内科、消化内科");
        hospital1.setWebsiteUrl("https://www.ql120.com");
        hospital1.setLatitude(30.657);
        hospital1.setLongitude(103.445);
        hospital1.setStatus(1);

        Hospital hospital2 = new Hospital();
        hospital2.setHospitalId("H002");
        hospital2.setHospitalName("西安交通大学第一附属医院");
        hospital2.setAddress("陕西省西安市雁塔区雁塔西路277号");
        hospital2.setPhone("029-85323240");
        hospital2.setLevel("三级甲等");
        hospital2.setDescription("西安交通大学第一附属医院是国家卫生健康委员会预算管理的大型综合三甲医院，是西北地区医疗、教学、科研、康复、预防保健的重要中心。");
        hospital2.setFlagshipDept("心血管内科、神经外科、肿瘤科、血液内科");
        hospital2.setWebsiteUrl("https://www.dyyfy.com");
        hospital2.setLatitude(34.23);
        hospital2.setLongitude(108.95);
        hospital2.setStatus(1);

        Hospital hospital3 = new Hospital();
        hospital3.setHospitalId("H003");
        hospital3.setHospitalName("西京医院");
        hospital3.setAddress("陕西省西安市新城区长乐西路127号");
        hospital3.setPhone("029-84774114");
        hospital3.setLevel("三级甲等");
        hospital3.setDescription("西京医院是空军军医大学第一附属医院，是一所集医疗、教学、科研和预防保健为一体的大型现代化综合性三级甲等医院。");
        hospital3.setFlagshipDept("烧伤外科、骨科、心脑血管病、眼科");
        hospital3.setWebsiteUrl("https://www.xijinghospital.com");
        hospital3.setLatitude(34.27);
        hospital3.setLongitude(108.97);
        hospital3.setStatus(1);

        Hospital hospital4 = new Hospital();
        hospital4.setHospitalId("H004");
        hospital4.setHospitalName("陕西省人民医院");
        hospital4.setAddress("陕西省西安市碑林区友谊西路256号");
        hospital4.setPhone("029-85251331");
        hospital4.setLevel("三级甲等");
        hospital4.setDescription("陕西省人民医院是陕西省政府举办的大型综合性三级甲等公立医院，是陕西省医疗、教学、科研和预防保健的重要基地。");
        hospital4.setFlagshipDept("老年医学、神经内外科、心血管内科");
        hospital4.setWebsiteUrl("https://www.sxhospital.com");
        hospital4.setLatitude(34.24);
        hospital4.setLongitude(108.94);
        hospital4.setStatus(1);

        Hospital hospital5 = new Hospital();
        hospital5.setHospitalId("H005");
        hospital5.setHospitalName("四川大学华西医院");
        hospital5.setAddress("四川省成都市武侯区国学巷37号");
        hospital5.setPhone("028-85422114");
        hospital5.setLevel("三级甲等");
        hospital5.setDescription("四川大学华西医院是中国西部疑难重症诊疗的国家级中心，是全国重要的医学人才培养基地和医学科技研发基地。");
        hospital5.setFlagshipDept("肝胆胰外科、骨科、呼吸内科、消化内科");
        hospital5.setWebsiteUrl("https://www.wchscu.cn");
        hospital5.setLatitude(30.65);
        hospital5.setLongitude(104.05);
        hospital5.setStatus(1);

        hospitalRepository.saveAll(java.util.Arrays.asList(hospital1, hospital2, hospital3, hospital4, hospital5));
        System.out.println("✓ 医院数据初始化完成（5家医院）");
    }

    private void initAdmins() {
        Admin admin = new Admin();
        admin.setAdminId("ADMIN001");
        admin.setUsername("admin");
        admin.setPassword("admin123");
        admin.setRealName("系统管理员");
        admin.setHospitalId(DEFAULT_HOSPITAL_ID);
        admin.setStatus(1);
        admin.setRole("SUPER_ADMIN");
        admin.setAuditStatus(1);

        adminRepository.save(admin);
        System.out.println("✓ 超级管理员数据初始化完成（admin/admin123）");
        
        initHospitalAdmins();
    }
    
    private void initHospitalAdmins() {
        Admin xiAnAdmin = new Admin();
        xiAnAdmin.setAdminId("ADMIN002");
        xiAnAdmin.setUsername("xian_admin");
        xiAnAdmin.setPassword("xian123");
        xiAnAdmin.setRealName("西安交大附院管理员");
        xiAnAdmin.setHospitalId("H002");
        xiAnAdmin.setStatus(1);
        xiAnAdmin.setRole("HOSPITAL_ADMIN");
        xiAnAdmin.setAuditStatus(1);
        
        Admin xijingAdmin = new Admin();
        xijingAdmin.setAdminId("ADMIN003");
        xijingAdmin.setUsername("xijing_admin");
        xijingAdmin.setPassword("xijing123");
        xijingAdmin.setRealName("西京医院管理员");
        xijingAdmin.setHospitalId("H003");
        xijingAdmin.setStatus(1);
        xijingAdmin.setRole("HOSPITAL_ADMIN");
        xijingAdmin.setAuditStatus(1);
        
        Admin shanxiAdmin = new Admin();
        shanxiAdmin.setAdminId("ADMIN004");
        shanxiAdmin.setUsername("shanxi_admin");
        shanxiAdmin.setPassword("shanxi123");
        shanxiAdmin.setRealName("陕西省人民医院管理员");
        shanxiAdmin.setHospitalId("H004");
        shanxiAdmin.setStatus(1);
        shanxiAdmin.setRole("HOSPITAL_ADMIN");
        shanxiAdmin.setAuditStatus(1);
        
        Admin huaxiAdmin = new Admin();
        huaxiAdmin.setAdminId("ADMIN005");
        huaxiAdmin.setUsername("huaxi_admin");
        huaxiAdmin.setPassword("huaxi123");
        huaxiAdmin.setRealName("华西医院管理员");
        huaxiAdmin.setHospitalId("H005");
        huaxiAdmin.setStatus(1);
        huaxiAdmin.setRole("HOSPITAL_ADMIN");
        huaxiAdmin.setAuditStatus(1);
        
        adminRepository.saveAll(java.util.Arrays.asList(xiAnAdmin, xijingAdmin, shanxiAdmin, huaxiAdmin));
        System.out.println("✓ 医院管理员数据初始化完成（4家医院）");
    }

    private void initQionglaiHospital() {
        String hospitalId = "H001";
        initDepartmentsForHospital(hospitalId);
        initQionglaiDoctors(hospitalId);
        initSchedulesForHospital(hospitalId);
    }

    private void initQionglaiDoctors(String hospitalId) {
        String deptPrefix = hospitalId + "_";

        Doctor doc1 = new Doctor();
        doc1.setDoctorId(nextDoctorId());
        doc1.setHospitalId(hospitalId);
        doc1.setName("王建国");
        doc1.setTitle("主任医师");
        doc1.setDeptId(deptPrefix + "DEPT001");
        doc1.setExpertise("急诊医学、危重症抢救、急性中毒救治");
        doc1.setGender("M");
        doc1.setUsername("qionglai_doctor001");
        doc1.setPassword("123456");
        doc1.setConsultationFee(50);
        doc1.setEducation("硕士研究生，毕业于四川大学华西医学中心");
        doc1.setMoral("从医20余年，始终坚持\"生命第一\"的原则，对每一位患者都尽职尽责。");
        doc1.setExperience("曾在四川大学华西医院急诊科进修，擅长各种急危重症的抢救处理。");

        Doctor doc2 = new Doctor();
        doc2.setDoctorId(nextDoctorId());
        doc2.setHospitalId(hospitalId);
        doc2.setName("李明华");
        doc2.setTitle("副主任医师");
        doc2.setDeptId(deptPrefix + "DEPT002");
        doc2.setExpertise("冠心病、高血压、心力衰竭、心律失常");
        doc2.setGender("F");
        doc2.setUsername("qionglai_doctor002");
        doc2.setPassword("123456");
        doc2.setConsultationFee(40);
        doc2.setEducation("本科毕业于重庆医科大学");
        doc2.setMoral("对待患者耐心细致，善于与患者沟通。");
        doc2.setExperience("从事心血管内科工作15年，熟练掌握各种急救技术。");

        Doctor doc3 = new Doctor();
        doc3.setDoctorId(nextDoctorId());
        doc3.setHospitalId(hospitalId);
        doc3.setName("张伟");
        doc3.setTitle("主任医师");
        doc3.setDeptId(deptPrefix + "DEPT003");
        doc3.setExpertise("胃炎、胃溃疡、肝炎、急性胰腺炎");
        doc3.setGender("M");
        doc3.setUsername("qionglai_doctor003");
        doc3.setPassword("123456");
        doc3.setConsultationFee(45);
        doc3.setEducation("博士研究生，毕业于四川大学华西医学中心");
        doc3.setMoral("坚持合理检查、合理用药原则，尽量减轻患者经济负担。");
        doc3.setExperience("从事消化内科临床工作25年，在消化道出血、急性胰腺炎等急症抢救方面经验丰富。");

        Doctor doc4 = new Doctor();
        doc4.setDoctorId(nextDoctorId());
        doc4.setHospitalId(hospitalId);
        doc4.setName("刘芳");
        doc4.setTitle("主治医师");
        doc4.setDeptId(deptPrefix + "DEPT004");
        doc4.setExpertise("骨折、关节疾病、运动损伤");
        doc4.setGender("F");
        doc4.setUsername("qionglai_doctor004");
        doc4.setPassword("123456");
        doc4.setConsultationFee(30);
        doc4.setEducation("硕士研究生，毕业于成都中医药大学");
        doc4.setMoral("耐心倾听患者诉求，注重功能康复和美观并重。");
        doc4.setExperience("从事骨科工作10年，擅长四肢骨折微创治疗。");

        doctorRepository.saveAll(java.util.Arrays.asList(doc1, doc2, doc3, doc4));
        System.out.println("✓ 邛崃市医疗中心医院医生数据初始化完成（4位医生）");
    }

    private void initXianJiaodaHospital() {
        String hospitalId = "H002";
        initDepartmentsForHospital(hospitalId);
        initXianJiaodaDoctors(hospitalId);
        initSchedulesForHospital(hospitalId);
    }

    private void initXianJiaodaDoctors(String hospitalId) {
        String deptPrefix = hospitalId + "_";

        Doctor doc1 = new Doctor();
        doc1.setDoctorId(nextDoctorId());
        doc1.setHospitalId(hospitalId);
        doc1.setName("王建安");
        doc1.setTitle("主任医师");
        doc1.setDeptId(deptPrefix + "DEPT002");
        doc1.setExpertise("冠心病、心律失常、心脏介入治疗");
        doc1.setGender("M");
        doc1.setUsername("wangjianan");
        doc1.setPassword("123456");
        doc1.setConsultationFee(60);
        doc1.setEducation("博士研究生，毕业于北京协和医学院");
        doc1.setMoral("从医30余年，在心血管领域具有崇高声誉，坚持以患者为中心。");
        doc1.setExperience("曾在美国梅奥诊所进修，擅长复杂冠心病的介入治疗和起搏器植入。");

        Doctor doc2 = new Doctor();
        doc2.setDoctorId(nextDoctorId());
        doc2.setHospitalId(hospitalId);
        doc2.setName("刘婷婷");
        doc2.setTitle("副主任医师");
        doc2.setDeptId(deptPrefix + "DEPT005");
        doc2.setExpertise("脑卒中、帕金森病、神经系统退行性疾病");
        doc2.setGender("F");
        doc2.setUsername("liutingting");
        doc2.setPassword("123456");
        doc2.setConsultationFee(45);
        doc2.setEducation("博士研究生，毕业于西安交通大学医学院");
        doc2.setMoral("对待患者细致入微，特别关注老年患者的生活质量。");
        doc2.setExperience("从事神经内科临床工作18年，在脑卒中急救和康复方面有丰富经验。");

        Doctor doc3 = new Doctor();
        doc3.setDoctorId(nextDoctorId());
        doc3.setHospitalId(hospitalId);
        doc3.setName("张西京");
        doc3.setTitle("主任医师");
        doc3.setDeptId(deptPrefix + "DEPT001");
        doc3.setExpertise("急危重症、多器官功能衰竭、创伤急救");
        doc3.setGender("M");
        doc3.setUsername("zhangxijing");
        doc3.setPassword("123456");
        doc3.setConsultationFee(55);
        doc3.setEducation("博士研究生，毕业于第四军医大学");
        doc3.setMoral("在多次重大公共卫生事件中冲锋在前，始终把患者生命放在第一位。");
        doc3.setExperience("从事急诊重症医学25年，是国内知名的急危重症专家。");

        Doctor doc4 = new Doctor();
        doc4.setDoctorId(nextDoctorId());
        doc4.setHospitalId(hospitalId);
        doc4.setName("陈晓红");
        doc4.setTitle("副主任医师");
        doc4.setDeptId(deptPrefix + "DEPT006");
        doc4.setExpertise("肺癌、肺炎、慢性阻塞性肺疾病");
        doc4.setGender("F");
        doc4.setUsername("chenxiaohong");
        doc4.setPassword("123456");
        doc4.setConsultationFee(40);
        doc4.setEducation("硕士研究生，毕业于西安交通大学");
        doc4.setMoral("致力于肿瘤患者的心理疏导，让患者在治疗过程中感受到温暖。");
        doc4.setExperience("从事呼吸内科工作15年，在肺癌早期诊断和靶向治疗方面有专长。");

        Doctor doc5 = new Doctor();
        doc5.setDoctorId(nextDoctorId());
        doc5.setHospitalId(hospitalId);
        doc5.setName("赵军");
        doc5.setTitle("主任医师");
        doc5.setDeptId(deptPrefix + "DEPT004");
        doc5.setExpertise("关节置换、脊柱外科、骨肿瘤");
        doc5.setGender("M");
        doc5.setUsername("zhaojun");
        doc5.setPassword("123456");
        doc5.setConsultationFee(50);
        doc5.setEducation("博士研究生，留学德国慕尼黑大学");
        doc5.setMoral("精于医术，诚于医德，每一台手术都精益求精。");
        doc5.setExperience("从事骨科临床工作22年，完成人工关节置换手术2000余例。");

        Doctor doc6 = new Doctor();
        doc6.setDoctorId(nextDoctorId());
        doc6.setHospitalId(hospitalId);
        doc6.setName("马丽娟");
        doc6.setTitle("主治医师");
        doc6.setDeptId(deptPrefix + "DEPT007");
        doc6.setExpertise("儿童呼吸系统疾病、新生儿保健、小儿发育评估");
        doc6.setGender("F");
        doc6.setUsername("malijuan");
        doc6.setPassword("123456");
        doc6.setConsultationFee(35);
        doc6.setEducation("硕士研究生，毕业于西安交通大学医学院");
        doc6.setMoral("对待孩子充满爱心，让小朋友不再害怕看医生。");
        doc6.setExperience("从事儿科工作12年，深受患儿家长的信赖和好评。");

        doctorRepository.saveAll(java.util.Arrays.asList(doc1, doc2, doc3, doc4, doc5, doc6));
        System.out.println("✓ 西安交通大学第一附属医院医生数据初始化完成（6位医生）");
    }

    private void initXijingHospital() {
        String hospitalId = "H003";
        initDepartmentsForHospital(hospitalId);
        initXijingDoctors(hospitalId);
        initSchedulesForHospital(hospitalId);
    }

    private void initXijingDoctors(String hospitalId) {
        String deptPrefix = hospitalId + "_";

        Doctor doc1 = new Doctor();
        doc1.setDoctorId(nextDoctorId());
        doc1.setHospitalId(hospitalId);
        doc1.setName("贾赤宇");
        doc1.setTitle("主任医师");
        doc1.setDeptId(deptPrefix + "DEPT004");
        doc1.setExpertise("烧伤外科、烧伤整形、慢性创面修复");
        doc1.setGender("M");
        doc1.setUsername("jiachiyu");
        doc1.setPassword("123456");
        doc1.setConsultationFee(60);
        doc1.setEducation("博士研究生，毕业于第四军医大学");
        doc1.setMoral("在大面积烧伤救治领域取得卓越成就，为无数患者重获新生。");
        doc1.setExperience("从事烧伤外科30年，是国际知名的烧伤外科专家。");

        Doctor doc2 = new Doctor();
        doc2.setDoctorId(nextDoctorId());
        doc2.setHospitalId(hospitalId);
        doc2.setName("雷伟");
        doc2.setTitle("主任医师");
        doc2.setDeptId(deptPrefix + "DEPT004");
        doc2.setExpertise("脊柱外科、颈椎病、腰椎间盘突出、脊柱畸形");
        doc2.setGender("M");
        doc2.setUsername("leiwei");
        doc2.setPassword("123456");
        doc2.setConsultationFee(55);
        doc2.setEducation("博士研究生，毕业于第四军医大学");
        doc2.setMoral("始终追求技术创新，为患者提供最先进的脊柱外科治疗方案。");
        doc2.setExperience("从事脊柱外科25年，完成脊柱手术5000余例。");

        Doctor doc3 = new Doctor();
        doc3.setDoctorId(nextDoctorId());
        doc3.setHospitalId(hospitalId);
        doc3.setName("王雨生");
        doc3.setTitle("主任医师");
        doc3.setDeptId(deptPrefix + "DEPT010");
        doc3.setExpertise("眼底病、白内障、青光眼、视网膜脱离");
        doc3.setGender("M");
        doc3.setUsername("wangyusheng");
        doc3.setPassword("123456");
        doc3.setConsultationFee(50);
        doc3.setEducation("博士研究生，毕业于第四军医大学");
        doc3.setMoral("用精湛医术为患者带来光明，是无数眼疾患者的希望之光。");
        doc3.setExperience("从事眼科临床工作28年，在眼底病和白内障手术方面造诣深厚。");

        Doctor doc4 = new Doctor();
        doc4.setDoctorId(nextDoctorId());
        doc4.setHospitalId(hospitalId);
        doc4.setName("刘丽英");
        doc4.setTitle("副主任医师");
        doc4.setDeptId(deptPrefix + "DEPT002");
        doc4.setExpertise("高血压、冠心病、心功能衰竭");
        doc4.setGender("F");
        doc4.setUsername("liuliying");
        doc4.setPassword("123456");
        doc4.setConsultationFee(45);
        doc4.setEducation("硕士研究生，毕业于第四军医大学");
        doc4.setMoral("细心耐心，是心血管患者的健康守护者。");
        doc4.setExperience("从事心血管内科工作18年，在老年心血管疾病管理方面有丰富经验。");

        doctorRepository.saveAll(java.util.Arrays.asList(doc1, doc2, doc3, doc4));
        System.out.println("✓ 西京医院医生数据初始化完成（4位医生）");
    }

    private void initShanxiRenminHospital() {
        String hospitalId = "H004";
        initDepartmentsForHospital(hospitalId);
        initShanxiRenminDoctors(hospitalId);
        initSchedulesForHospital(hospitalId);
    }

    private void initShanxiRenminDoctors(String hospitalId) {
        String deptPrefix = hospitalId + "_";

        Doctor doc1 = new Doctor();
        doc1.setDoctorId(nextDoctorId());
        doc1.setHospitalId(hospitalId);
        doc1.setName("张建华");
        doc1.setTitle("主任医师");
        doc1.setDeptId(deptPrefix + "DEPT002");
        doc1.setExpertise("老年心血管病、高血压、冠心病");
        doc1.setGender("M");
        doc1.setUsername("zhangjianhua");
        doc1.setPassword("123456");
        doc1.setConsultationFee(50);
        doc1.setEducation("博士研究生，毕业于西安交通大学医学院");
        doc1.setMoral("专注老年医学20余年，是陕西省老年医学领域的领军人物。");
        doc1.setExperience("从事老年心血管病临床工作25年，在老年多病共存的综合管理方面经验丰富。");

        Doctor doc2 = new Doctor();
        doc2.setDoctorId(nextDoctorId());
        doc2.setHospitalId(hospitalId);
        doc2.setName("李小英");
        doc2.setTitle("副主任医师");
        doc2.setDeptId(deptPrefix + "DEPT005");
        doc2.setExpertise("脑血管病、头痛、眩晕、失眠");
        doc2.setGender("F");
        doc2.setUsername("lixiaoying");
        doc2.setPassword("123456");
        doc2.setConsultationFee(40);
        doc2.setEducation("硕士研究生，毕业于西安交通大学");
        doc2.setMoral("关注患者生活质量，帮助患者重获健康。");
        doc2.setExperience("从事神经内科工作15年，在脑卒中康复方面有专长。");

        Doctor doc3 = new Doctor();
        doc3.setDoctorId(nextDoctorId());
        doc3.setHospitalId(hospitalId);
        doc3.setName("王大军");
        doc3.setTitle("主任医师");
        doc3.setDeptId(deptPrefix + "DEPT003");
        doc3.setExpertise("胃肠疾病、肝病、消化道肿瘤");
        doc3.setGender("M");
        doc3.setUsername("wangdajun");
        doc3.setPassword("123456");
        doc3.setConsultationFee(55);
        doc3.setEducation("博士研究生，毕业于北京大学医学部");
        doc3.setMoral("医术精湛，医德高尚，深受患者信赖。");
        doc3.setExperience("从事消化内科临床工作22年，在消化道早期癌症诊断和治疗方面经验丰富。");

        doctorRepository.saveAll(java.util.Arrays.asList(doc1, doc2, doc3));
        System.out.println("✓ 陕西省人民医院医生数据初始化完成（3位医生）");
    }

    private void initHuaxiHospital() {
        String hospitalId = "H005";
        initDepartmentsForHospital(hospitalId);
        initHuaxiDoctors(hospitalId);
        initSchedulesForHospital(hospitalId);
    }

    private void initHuaxiDoctors(String hospitalId) {
        String deptPrefix = hospitalId + "_";

        Doctor doc1 = new Doctor();
        doc1.setDoctorId(nextDoctorId());
        doc1.setHospitalId(hospitalId);
        doc1.setName("严律南");
        doc1.setTitle("主任医师");
        doc1.setDeptId(deptPrefix + "DEPT004");
        doc1.setExpertise("肝脏移植、肝癌、肝门部胆管癌、胰腺癌");
        doc1.setGender("M");
        doc1.setUsername("yanlunan");
        doc1.setPassword("123456");
        doc1.setConsultationFee(80);
        doc1.setEducation("博士研究生，毕业于华西医科大学");
        doc1.setMoral("中国肝脏移植领域的开拓者，为无数终末期肝病患者带来新生。");
        doc1.setExperience("从事肝胆胰外科临床工作40年，完成肝移植手术1000余例。");

        Doctor doc2 = new Doctor();
        doc2.setDoctorId(nextDoctorId());
        doc2.setHospitalId(hospitalId);
        doc2.setName("裴福兴");
        doc2.setTitle("主任医师");
        doc2.setDeptId(deptPrefix + "DEPT004");
        doc2.setExpertise("关节外科、人工关节置换、骨关节炎");
        doc2.setGender("M");
        doc2.setUsername("peifuxing");
        doc2.setPassword("123456");
        doc2.setConsultationFee(60);
        doc2.setEducation("博士研究生，毕业于华西医科大学");
        doc2.setMoral("中国关节外科领域的权威专家，让无数关节疾病患者重新站立行走。");
        doc2.setExperience("从事骨科临床工作35年，完成人工关节置换手术5000余例。");

        Doctor doc3 = new Doctor();
        doc3.setDoctorId(nextDoctorId());
        doc3.setHospitalId(hospitalId);
        doc3.setName("李为民");
        doc3.setTitle("主任医师");
        doc3.setDeptId(deptPrefix + "DEPT006");
        doc3.setExpertise("肺癌、慢性阻塞性肺疾病、呼吸衰竭");
        doc3.setGender("M");
        doc3.setUsername("liweimin");
        doc3.setPassword("123456");
        doc3.setConsultationFee(65);
        doc3.setEducation("博士研究生，毕业于华西医科大学");
        doc3.setMoral("华西医院呼吸内科的领军人物，在肺癌早期诊断和精准治疗方面造诣深厚。");
        doc3.setExperience("从事呼吸内科临床工作30年，是国内知名的呼吸病学专家。");

        Doctor doc4 = new Doctor();
        doc4.setDoctorId(nextDoctorId());
        doc4.setHospitalId(hospitalId);
        doc4.setName("唐承薇");
        doc4.setTitle("主任医师");
        doc4.setDeptId(deptPrefix + "DEPT003");
        doc4.setExpertise("胃肠疾病、功能性胃肠病、胰腺疾病");
        doc4.setGender("F");
        doc4.setUsername("tangchengwei");
        doc4.setPassword("123456");
        doc4.setConsultationFee(55);
        doc4.setEducation("博士研究生，毕业于华西医科大学");
        doc4.setMoral("在功能性胃肠病领域有独到见解，为无数患者解除了病痛。");
        doc4.setExperience("从事消化内科临床工作28年，在功能性胃肠病和胰腺疾病方面经验丰富。");

        Doctor doc5 = new Doctor();
        doc5.setDoctorId(nextDoctorId());
        doc5.setHospitalId(hospitalId);
        doc5.setName("周清华");
        doc5.setTitle("主任医师");
        doc5.setDeptId(deptPrefix + "DEPT004");
        doc5.setExpertise("肺癌外科治疗、胸部肿瘤、微创外科");
        doc5.setGender("M");
        doc5.setUsername("zhouqinghua");
        doc5.setPassword("123456");
        doc5.setConsultationFee(70);
        doc5.setEducation("博士研究生，毕业于华西医科大学");
        doc5.setMoral("中国肺癌外科治疗领域的顶尖专家，为无数肺癌患者带来希望。");
        doc5.setExperience("从事胸外科临床工作32年，完成肺癌手术6000余例。");

        Doctor doc6 = new Doctor();
        doc6.setDoctorId(nextDoctorId());
        doc6.setHospitalId(hospitalId);
        doc6.setName("母得志");
        doc6.setTitle("主任医师");
        doc6.setDeptId(deptPrefix + "DEPT007");
        doc6.setExpertise("新生儿疾病、儿童呼吸系统疾病、小儿重症医学");
        doc6.setGender("M");
        doc6.setUsername("mudezhi");
        doc6.setPassword("123456");
        doc6.setConsultationFee(50);
        doc6.setEducation("博士研究生，毕业于华西医科大学");
        doc6.setMoral("华西医院儿科的领军人物，是无数患儿的健康守护者。");
        doc6.setExperience("从事儿科临床工作25年，在新生儿重症监护方面经验丰富。");

        doctorRepository.saveAll(java.util.Arrays.asList(doc1, doc2, doc3, doc4, doc5, doc6));
        System.out.println("✓ 四川大学华西医院医生数据初始化完成（6位医生）");
    }

    private void initDepartmentsForHospital(String hospitalId) {
        String prefix = hospitalId + "_";
        
        Department dept1 = new Department();
        dept1.setDeptId(prefix + "DEPT001");
        dept1.setHospitalId(hospitalId);
        dept1.setDeptName("急诊科");
        dept1.setDescription("急诊科是医院急危重症患者抢救的第一线，配备先进的急救设备和专业的急救医疗团队。");
        dept1.setDiseases("急性心肌梗死、急性脑卒中、严重创伤、急性呼吸衰竭、急性中毒、高热惊厥、急性腹痛、重症感染、休克");
        dept1.setServiceTime("24小时");
        dept1.setIsFlagship(1);

        Department dept2 = new Department();
        dept2.setDeptId(prefix + "DEPT002");
        dept2.setHospitalId(hospitalId);
        dept2.setDeptName("心血管内科");
        dept2.setDescription("心血管内科是医院重点发展学科，擅长心血管系统疾病的诊断与治疗。");
        dept2.setDiseases("冠心病、心绞痛、心肌梗死、高血压、心力衰竭、心律失常、先天性心脏病、风湿性心脏病");
        dept2.setServiceTime("周一至周日 08:00-21:30");
        dept2.setIsFlagship(1);

        Department dept3 = new Department();
        dept3.setDeptId(prefix + "DEPT003");
        dept3.setHospitalId(hospitalId);
        dept3.setDeptName("消化内科");
        dept3.setDescription("消化内科主要诊治消化系统疾病，科室技术力量雄厚。");
        dept3.setDiseases("胃炎、胃溃疡、十二指肠溃疡、慢性乙肝、丙肝、肝硬化、急性胰腺炎、胆囊炎、胆结石、炎症性肠病");
        dept3.setServiceTime("周一至周日 08:00-21:30");
        dept3.setIsFlagship(1);

        Department dept4 = new Department();
        dept4.setDeptId(prefix + "DEPT004");
        dept4.setHospitalId(hospitalId);
        dept4.setDeptName("骨科");
        dept4.setDescription("骨科是医院重点科室之一，擅长各类骨折、关节疾病、脊柱疾病的诊治。");
        dept4.setDiseases("骨折、关节脱位、骨关节炎、类风湿性关节炎、颈椎病、腰椎间盘突出症、脊柱骨折、骨质疏松症");
        dept4.setServiceTime("周一至周日 08:00-21:30");

        Department dept5 = new Department();
        dept5.setDeptId(prefix + "DEPT005");
        dept5.setHospitalId(hospitalId);
        dept5.setDeptName("神经内科");
        dept5.setDescription("神经内科主要诊治脑血管疾病、神经系统感染性疾病、锥体外系疾病等。");
        dept5.setDiseases("脑卒中、脑梗死、脑出血、偏头痛、紧张性头痛、眩晕症、癫痫、帕金森病、阿尔茨海默病、面瘫");
        dept5.setServiceTime("周一至周五 08:00-17:30");

        Department dept6 = new Department();
        dept6.setDeptId(prefix + "DEPT006");
        dept6.setHospitalId(hospitalId);
        dept6.setDeptName("呼吸内科");
        dept6.setDescription("呼吸内科诊治各类呼吸系统疾病，在慢性阻塞性肺疾病、支气管哮喘、肺炎等疾病的诊疗方面经验丰富。");
        dept6.setDiseases("感冒、流感、肺炎、支气管炎、慢性阻塞性肺疾病(COPD)、支气管哮喘、肺结核、肺癌、胸腔积液");
        dept6.setServiceTime("周一至周五 08:00-17:30");

        Department dept7 = new Department();
        dept7.setDeptId(prefix + "DEPT007");
        dept7.setHospitalId(hospitalId);
        dept7.setDeptName("儿科");
        dept7.setDescription("儿科是诊治小儿疾病的综合科室，由经验丰富的儿科专家坐诊。");
        dept7.setDiseases("上呼吸道感染、支气管肺炎、腹泻病、小儿惊厥、过敏性疾病、贫血、佝偻病、手足口病、水痘、麻疹");
        dept7.setServiceTime("周一至周五 08:30-12:00, 14:00-17:30");

        Department dept8 = new Department();
        dept8.setDeptId(prefix + "DEPT008");
        dept8.setHospitalId(hospitalId);
        dept8.setDeptName("妇产科");
        dept8.setDescription("妇产科提供女性生殖健康、孕产妇保健、妇科疾病诊治等服务。");
        dept8.setDiseases("月经不调、痛经、子宫肌瘤、卵巢囊肿、宫颈炎、阴道炎、不孕症、先兆流产、围绝经期综合征");
        dept8.setServiceTime("周一至周五 08:30-12:00, 14:00-17:30");

        Department dept9 = new Department();
        dept9.setDeptId(prefix + "DEPT009");
        dept9.setHospitalId(hospitalId);
        dept9.setDeptName("口腔科");
        dept9.setDescription("口腔科提供口腔检查、补牙、拔牙、牙周治疗、口腔修复等口腔医疗服务。");
        dept9.setDiseases("龋齿、牙髓炎、根尖周炎、牙周炎、牙龈炎、口腔溃疡、智齿冠周炎、牙外伤、牙列缺损");
        dept9.setServiceTime("周一至周五 08:30-12:00, 14:00-17:30");

        Department dept10 = new Department();
        dept10.setDeptId(prefix + "DEPT010");
        dept10.setHospitalId(hospitalId);
        dept10.setDeptName("眼科");
        dept10.setDescription("眼科诊治各类眼科疾病，在白内障、青光眼、视网膜疾病等方面具有丰富经验。");
        dept10.setDiseases("白内障、青光眼、视网膜脱离、糖尿病视网膜病变、黄斑病变、角膜炎、结膜炎、干眼症、近视、远视");
        dept10.setServiceTime("周一至周五 08:30-12:00, 14:00-17:30");

        Department dept11 = new Department();
        dept11.setDeptId(prefix + "DEPT011");
        dept11.setHospitalId(hospitalId);
        dept11.setDeptName("中医科");
        dept11.setDescription("中医科开展中医辨证论治、针灸推拿、中药调理等传统中医诊疗服务。");
        dept11.setDiseases("感冒、咳嗽、失眠、头痛、胃肠功能紊乱、慢性疲劳综合征、颈椎病、腰腿痛、面瘫、中风后遗症");
        dept11.setServiceTime("周一至周五 08:30-12:00, 14:00-17:30");

        Department dept12 = new Department();
        dept12.setDeptId(prefix + "DEPT012");
        dept12.setHospitalId(hospitalId);
        dept12.setDeptName("预防保健科");
        dept12.setDescription("预防保健科提供健康体检、预防接种、健康教育等服务。");
        dept12.setDiseases("健康体检、预防接种、健康咨询、疾病筛查、健康管理");
        dept12.setServiceTime("周一至周五 08:30-12:00, 14:00-17:30");

        departmentRepository.saveAll(java.util.Arrays.asList(
            dept1, dept2, dept3, dept4, dept5, dept6, dept7, dept8, dept9, dept10, dept11, dept12
        ));
        System.out.println("✓ 科室数据初始化完成（" + hospitalId + " - 12个科室）");
    }

    private void initSchedulesForHospital(String hospitalId) {
        LocalDate today = LocalDate.now();
        List<Doctor> doctors = doctorRepository.findByHospitalId(hospitalId);

        if (doctors.isEmpty()) {
            System.out.println("! 排班初始化跳过（" + hospitalId + " 无医生）");
            return;
        }

        // 与正式排班规则一致：只生成正常工作时段（默认 08:00-12:00、14:00-17:30），20 分钟一段
        List<String> timeSlots = scheduleRules.timeSlots();

        int scheduleCount = 0;
        for (int day = 0; day < 7; day++) {
            LocalDate date = today.plusDays(day);
            for (String timeSlot : timeSlots) {
                Schedule schedule = new Schedule();
                schedule.setScheduleId(java.util.UUID.randomUUID().toString().replace("-", ""));
                schedule.setHospitalId(hospitalId);
                schedule.setDoctorId(doctors.get(scheduleCount % doctors.size()).getDoctorId());
                schedule.setWorkDate(date);
                schedule.setTimeSlot(timeSlot);
                schedule.setMaxPatients(scheduleRules.getMaxPatientsPerSlot());
                schedule.setBookedCount(0);
                schedule.setStatus(1);
                scheduleRepository.save(schedule);
                scheduleCount++;
            }
        }

        System.out.println("✓ 排班数据初始化完成（" + hospitalId + " - " + scheduleCount + "条排班，时段 "
                + scheduleRules.getWorkHoursText() + "）");
    }

    private void initPatients() {
        Patient patient1 = new Patient();
        patient1.setPatientId("P138001");
        patient1.setName("测试用户1");
        patient1.setGender("M");
        patient1.setAge(24);
        patient1.setPhone("13800138001");
        patient1.setMedicalHistory("无");

        Patient patient2 = new Patient();
        patient2.setPatientId("P138002");
        patient2.setName("测试用户2");
        patient2.setGender("F");
        patient2.setAge(23);
        patient2.setPhone("13800138002");
        patient2.setMedicalHistory("无");

        patientRepository.saveAll(java.util.Arrays.asList(patient1, patient2));
        System.out.println("✓ 患者数据初始化完成（2位测试患者）");
    }
}
