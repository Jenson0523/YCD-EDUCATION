package com.yunchendun.modules.leave.service;

import com.yunchendun.modules.leave.domain.FaceConsentLog;
import com.yunchendun.modules.leave.mapper.FaceConsentLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模块: 人脸识别请假离校
 * 功能: 人脸采集合规——《告知同意书》内容管理 + 授权留痕
 *
 * 依据《个人信息保护法》(PIPL)：人脸信息属敏感个人信息，处理前须"单独同意"；
 * 不满14周岁未成年人的个人信息须取得监护人同意。
 */
@Service
@RequiredArgsConstructor
public class FaceConsentService {

    private final FaceConsentLogMapper consentLogMapper;

    /** 同意书版本号——内容修订时必须递增，旧版授权依然可追溯 */
    public static final String CONSENT_VERSION = "v1.0-202606";

    private static final String CONSENT_TITLE = "学生人脸信息采集与使用告知同意书";

    private static final String[] CONSENT_SECTIONS = {
            "一、采集目的\n为保障学生在校安全，本系统在学生请假离校环节使用人脸识别进行身份核验（门卫刷脸放行），防止冒领、错放等安全事件，并为家校双方提供离校留痕凭证。人脸信息仅用于上述离校安全核验目的，不用于任何其他用途。",

            "二、采集的信息内容\n1. 学生正面人脸照片一张；\n2. 由照片生成的人脸特征数据（仅用于比对，无法还原为原始照片）；\n3. 关联的学生基本信息（姓名、学籍号、班级）。",

            "三、信息的存储与保护\n1. 人脸照片与特征数据仅存储于学校自有服务器，不上传任何第三方云端人脸库；\n2. 人脸比对采用本地离线引擎完成，比对过程不向外部传输人脸数据；\n3. 人脸照片访问受严格权限控制：仅管理员、相关班主任、绑定监护人及门卫核验场景可访问，所有访问均需登录鉴权；\n4. 系统对采集、修改、访问行为留存审计日志。",

            "四、保存期限\n人脸信息自采集之日起保存至学生毕业、转学或退学。学籍状态终止后30日内，系统将删除该学生的人脸照片与特征数据。",

            "五、您的权利\n1. 您有权查询、复制为您孩子采集的人脸信息；\n2. 您有权要求更正或更新人脸照片；\n3. 您有权随时撤回本同意：撤回后系统将删除人脸信息并停止刷脸核验功能（离校核验将改用人工核验），撤回不影响撤回前基于授权已开展的处理活动；\n4. 行使上述权利请联系班主任或学校管理员。",

            "六、监护人同意\n被采集学生为未成年人。若学生不满14周岁，须由其监护人阅读本同意书并作出同意；若学生已满14周岁，建议监护人与学生共同阅读并同意。教师代为采集的，应当事先取得监护人的明确授权。",

            "七、拒绝同意的后果\n人脸采集遵循自愿原则。若不同意采集，学生仍可正常请假离校，但离校核验将采用人工核对方式（出示假条+门卫人工确认），可能耗时略长。",

            "八、其他\n本同意书的解释权归学校所有。如本系统人脸信息处理方式发生变更，学校将更新本同意书并重新征得您的同意。"
    };

    /** 返回同意书全文（小程序/PC展示用） */
    public Map<String, Object> getConsentDoc() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("version", CONSENT_VERSION);
        doc.put("title", CONSENT_TITLE);
        doc.put("sections", CONSENT_SECTIONS);
        doc.put("footer", "请完整阅读以上内容。点击「我已阅读并同意」即表示您已知悉并同意上述全部条款。");
        return doc;
    }

    /** 记录一次授权同意（审计留痕） */
    public void recordConsent(Long studentId, String studentNo, String studentName,
                              Long userId, String userName, String role, String action) {
        FaceConsentLog log = new FaceConsentLog();
        log.setTenantId(1L);
        log.setStudentId(studentId);
        log.setStudentNo(studentNo);
        log.setStudentName(studentName);
        log.setConsentVersion(CONSENT_VERSION);
        log.setAgreeUserId(userId);
        log.setAgreeUserName(userName);
        log.setAgreeRole(role);
        log.setAction(action);
        log.setCreatedAt(LocalDateTime.now());
        consentLogMapper.insert(log);
    }
}
