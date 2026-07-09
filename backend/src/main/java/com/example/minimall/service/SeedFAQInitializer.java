package com.example.minimall.service;

import com.example.minimall.mapper.KnowledgeFaqMapper;
import com.example.minimall.model.KnowledgeFaq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 冷启动解决方案：种子FAQ初始化 + 知识库覆盖率监控
 *
 * 功能：
 * 1. 预置50+条高频业务问答种子数据
 * 2. 知识库覆盖率量化指标计算
 * 3. 低覆盖率告警提示
 */
@Service
public class SeedFAQInitializer {
    private static final Logger logger = LoggerFactory.getLogger(SeedFAQInitializer.class);

    @Autowired
    private KnowledgeFaqMapper faqMapper;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorStoreService vectorStoreService;

    /** 覆盖率告警阈值 */
    private static final double COVERAGE_ALERT_THRESHOLD = 0.70;

    /** 种子FAQ数据 */
    private static final List<SeedFAQ> SEED_FAQS = Arrays.asList(
        // ==================== 售后政策类（10条） ====================
        new SeedFAQ("退换货政策是什么",
                "我们提供7天无理由退换货服务。商品签收后7天内，如商品存在质量问题或不符合描述，可申请退换货。退换货流程：1.在订单页面点击申请售后；2.填写退换货原因并上传凭证；3.客服审核通过后寄回商品；4.收到商品后3个工作日内完成退款。运费承担：质量问题由卖家承担，非质量问题由买家承担。",
                "售后政策", "退换货,退款,售后,7天无理由", 10),
        new SeedFAQ("退款多久到账",
                "退款一般在收到退回商品并验收通过后的1-3个工作日内原路退回。微信支付/支付宝退款即时到账，银行卡退款可能需要3-5个工作日。如有延迟请联系客服查询订单退款进度。",
                "售后政策", "退款,到账时间,订单", 9),
        new SeedFAQ("如何申请退款",
                "申请退款流程：1.进入我的订单页面；2.找到对应订单点击申请售后；3.选择退款原因并填写说明；4.上传商品问题凭证照片；5.提交后等待客服审核。客服会在24小时内处理您的售后申请，审核通过后即进入退款流程。",
                "售后政策", "退款,售后,申请流程", 9),
        new SeedFAQ("质量问题如何退换",
                "如收到的商品存在质量问题，请在签收后48小时内拍照取证并联系客服申请退换货。需提供商品问题照片、订单号、问题描述等证据。客服审核通过后将安排免费上门取件，验收合格后重新发货或退款。质量问题产生的运费由卖家全额承担。",
                "售后政策", "退换货,质量,售后,质量问题", 9),
        new SeedFAQ("退换货运费谁承担",
                "退换货运费承担规则：1.因商品质量问题导致的退换货，运费由卖家承担；2.非质量问题（如个人喜好）的退换货，运费由买家承担；3.七天无理由退货，运费由买家承担。建议退回前与客服确认运费责任，保留运费凭证以便后续报销。",
                "售后政策", "退换货,运费,售后", 8),
        new SeedFAQ("发票怎么开具",
                "发票开具流程：1.下单时在订单备注中填写发票抬头和税号信息；2.也可在订单完成后联系客服补开；3.我们支持电子发票和纸质发票两种形式；4.电子发票将在订单完成后24小时内发送至您预留的邮箱；5.纸质发票将在7个工作日内寄出。发票内容默认为商品明细，可开具增值税普通发票或专用发票。",
                "售后政策", "发票,订单,开票", 7),
        new SeedFAQ("商品保修期多久",
                "商品保修期根据品类不同有所差异：1.生鲜农产品类商品因保质期较短，不支持长期保修，但保证新鲜度；2.加工类农产品（如蜂蜜、干货）保质期内如有质量问题可申请售后；3.平台商品默认提供7天质量保障期，期间出现非人为质量问题可免费退换。具体保修政策以商品详情页说明为准。",
                "售后政策", "保修,售后,质量保障", 7),
        new SeedFAQ("如何投诉商家",
                "投诉商家流程：1.在订单详情页点击投诉商家按钮；2.选择投诉类型（商品质量问题、服务态度、虚假宣传等）；3.详细填写投诉内容并上传证据；4.提交后平台客服将在48小时内介入处理。我们承诺保护投诉人隐私，对查实的违规商家将进行处罚。您也可拨打客服热线进行电话投诉。",
                "售后政策", "投诉,售后,商家投诉", 7),
        new SeedFAQ("订单取消后多久退款",
                "订单取消后的退款时效：1.未支付的订单可直接取消，无需退款；2.已支付未发货的订单，取消后1-2个工作日内原路退款；3.已发货订单需拒收或退货后才能退款；4.使用优惠券的订单取消后，优惠券将自动退回账户（若未过期）。退款进度可在订单详情页实时查询。",
                "售后政策", "退款,订单,取消订单", 8),
        new SeedFAQ("售后服务时间是几点",
                "我们的售后服务时间为每日8:00-22:00，全年无休。您可通过以下方式联系售后客服：1.在线客服（推荐，响应最快）；2.客服热线400-XXX-XXXX；3.订单页面提交售后申请。非工作时间的售后申请将于次日优先处理。紧急问题可留言，客服上线后将第一时间回复。",
                "售后政策", "售后,服务时间,客服", 5),

        // ==================== 配送物流类（10条） ====================
        new SeedFAQ("配送范围有哪些",
                "我们支持全国配送（除港澳台地区部分偏远地区外）。大部分地区支持次日达，偏远地区3-5天送达。部分生鲜产品因保鲜要求，配送范围可能受限，详见商品页面说明。新疆、西藏、青海等部分偏远地区可能需要额外运费和配送时间，具体以结算页提示为准。",
                "配送物流", "配送,范围,全国,物流", 8),
        new SeedFAQ("商品多久能送到",
                "配送时效说明：1.同城订单一般当日或次日送达；2.省内订单1-2天送达；3.省外订单2-4天送达；4.偏远地区3-5天送达。生鲜类商品采用冷链物流，优先发货保证新鲜。如遇节假日或恶劣天气，配送时间可能延长。您可在订单详情页实时查看物流进度。",
                "配送物流", "配送,物流,送货时间", 8),
        new SeedFAQ("物流信息怎么查询",
                "物流查询方式：1.登录账号进入我的订单，点击对应订单查看物流详情；2.在物流跟踪页面可看到实时位置和配送节点；3.我们会在商品发货后发送短信通知物流单号；4.也可通过快递公司官网或APP用单号查询。如物流信息长时间未更新，请联系客服协助查询。",
                "配送物流", "物流,查询,订单", 7),
        new SeedFAQ("配送费用怎么计算",
                "配送费用规则：1.单笔订单满99元包邮（部分偏远地区除外）；2.不满99元收取8-15元运费，根据收货地区而定；3.生鲜冷链商品统一收取15元冷链费；4.大件商品运费根据体积重量单独计算。运费明细会在结算页面清晰展示，使用优惠券可抵扣运费。",
                "配送物流", "配送,物流,运费,包邮", 7),
        new SeedFAQ("偏远地区能配送吗",
                "偏远地区配送说明：1.我们支持新疆、西藏、青海、内蒙古等偏远地区配送；2.偏远地区可能需要额外收取15-30元运费；3.配送时效延长至5-7个工作日；4.部分生鲜保鲜商品因物流时效限制可能无法配送至偏远地区；5.具体配送范围和费用以下单时系统校验为准。建议偏远地区用户优先选择干货类商品。",
                "配送物流", "配送,偏远地区,物流", 6),
        new SeedFAQ("支持货到付款吗",
                "支付方式说明：1.我们目前主要支持在线支付（微信支付、支付宝、银行卡）；2.部分地区支持货到付款服务，下单时系统会提示是否可用；3.货到付款仅支持现金支付，可能收取少量手续费；4.推荐使用在线支付，可享受支付优惠并提升订单处理速度。大额订单建议使用在线支付更安全。",
                "配送物流", "支付,配送,货到付款", 6),
        new SeedFAQ("商品什么时候发货",
                "发货时间说明：1.现货商品下单后24小时内发货（工作日）；2.预售商品按页面标注的发货时间发货；3.定制类商品需与商家协商发货时间；4.生鲜商品为保证新鲜，采用产地直发模式，一般凌晨采摘当日发货；5.节假日订单可能延迟至节后首个工作日发货。发货后您会收到短信通知。",
                "配送物流", "商品,物流,发货时间", 7),
        new SeedFAQ("可以指定配送时间吗",
                "指定配送时间服务：1.部分城市支持预约配送时间，下单时可在备注中填写期望送达时间段；2.可选时间段包括工作日、周末、上午、下午等；3.预约配送可能因物流运力原因无法100%保证，我们会尽量满足；4.生鲜商品因保鲜要求不支持长时间预约。如需特定时间送达，建议提前1-2天下单并备注说明。",
                "配送物流", "配送,预约,物流", 5),
        new SeedFAQ("物流公司有哪些",
                "我们合作的物流公司包括：1.顺丰速运（生鲜冷链首选，速度快）；2.中通快递（覆盖广，性价比高）；3.圆通速递；4.韵达快递；5.京东物流；6.EMS（偏远地区配送）。系统会根据商品类型、收货地址自动选择最优物流。您也可在订单备注中指定物流公司，我们会尽量安排（可能影响运费）。",
                "配送物流", "物流,快递,物流公司", 5),
        new SeedFAQ("收货地址填错了怎么改",
                "修改收货地址说明：1.订单未发货前，可在订单详情页直接修改收货地址；2.已发货订单无法修改地址，需联系物流公司拦截或转寄；3.如因地址错误导致退回，需承担二次配送运费；4.建议下单时仔细核对地址信息；5.常用地址可保存至地址簿方便下次使用。如有特殊情况请联系客服协助处理。",
                "配送物流", "订单,配送,收货地址", 6),

        // ==================== 商品质量类（10条） ====================
        new SeedFAQ("商品质量如何保证",
                "我们严格把控商品质量：1.所有商家需通过资质认证；2.农产品需提供检测报告；3.平台定期抽检；4.用户评价系统监督。如发现质量问题，可申请退换货并获得赔偿。我们承诺所售商品均为正品，对质量问题零容忍，一经查实将下架商品并处罚商家。",
                "商品质量", "质量,保证,检测,商品", 8),
        new SeedFAQ("农产品有检测报告吗",
                "农产品检测说明：1.所有入驻商家需提供农产品质量安全检测报告；2.检测项目包括农药残留、重金属含量、微生物指标等；3.有机认证商品需提供有机认证证书；4.检测报告可在商品详情页查看；5.平台不定期进行第三方抽检验证。我们坚持源头把控，确保每一份农产品都安全放心。",
                "商品质量", "质量,认证,检测报告,农产品", 7),
        new SeedFAQ("商品价格怎么定的",
                "商品定价机制：1.农产品价格由农户根据种植成本、市场行情定价；2.平台不收取高额中间费用，让利农户和消费者；3.季节性商品价格随市场供需浮动；4.助农补贴商品享受平台补贴价；5.批量采购可享阶梯优惠。我们坚持透明定价，商品详情页展示价格构成，让您明白消费。",
                "商品质量", "价格,商品,定价,助农", 6),
        new SeedFAQ("商品库存不足怎么办",
                "库存不足处理方案：1.商品售罄时可在商品页点击到货通知，补货后第一时间提醒您；2.部分商品支持预售，可提前下单锁定货源；3.同类商品可查看推荐替代品；4.生鲜农产品因季节性强，建议应季时及时购买；5.如急需可联系客服查询是否有其他规格或相似商品。我们会根据销量预测积极协调农户补货。",
                "商品质量", "库存,商品,补货,预售", 6),
        new SeedFAQ("商品什么时候上新",
                "商品上新说明：1.应季农产品在上市季节会陆续上新，如春季草莓、夏季西瓜、秋季苹果等；2.新品上架前会通过首页推荐、消息推送通知用户；3.新品常伴有尝鲜优惠活动；4.关注感兴趣的商家或分类可及时获取上新信息；5.平台每周精选新品在专区展示。时令农产品建议关注上新时间，错过需等来年。",
                "商品质量", "上架,商品,新品,上新", 5),
        new SeedFAQ("临期商品怎么处理",
                "临期商品处理规则：1.临期商品会在详情页明确标注保质期信息并降价销售；2.生鲜商品临近保质期会进行清仓特卖；3.干货运费类商品保质期不足1/3时折扣销售；4.临期商品不支持无理由退货，请谨慎购买；5.已过期商品立即下架销毁，绝不销售。我们保证所有在售商品均在保质期内，确保食品安全。",
                "商品质量", "商品,质量,保质期,临期", 6),
        new SeedFAQ("有机认证怎么查询",
                "有机认证查询方法：1.有机认证商品在详情页展示认证证书和编号；2.可登录中国食品农产品认证信息系统，输入认证编号验证真伪；3.我们要求商家每年更新认证证书；4.平台仅展示经国家认监委批准的认证机构颁发的证书；5.如发现虚假认证可举报，核实后奖励举报人。有机食品价格较高但更安全健康。",
                "商品质量", "认证,质量,有机认证,查询", 6),
        new SeedFAQ("农产品产地可以追溯吗",
                "产地追溯系统：1.每件农产品都配有追溯二维码，扫码可查看产地、种植户、采摘日期等信息；2.追溯信息包含种植过程中的施肥、用药记录；3.加工类商品可追溯到原料产地和加工工厂；4.我们建立全链条溯源体系，从田间到餐桌全程可查；5.如发现信息不符可联系客服核实。溯源系统让您买得放心、吃得安心。",
                "商品质量", "质量,追溯,产地,农产品", 6),
        new SeedFAQ("商品规格怎么看",
                "商品规格说明：1.每个商品详情页都有规格说明区，标注重量、数量、等级等信息；2.生鲜农产品按重量计价（如500g/份）；3.礼盒装商品标注内含商品明细；4.部分商品提供多种规格选择（如1斤装、2斤装）；5.规格如有疑问可咨询在线客服。建议购买前仔细阅读规格说明，避免收到商品后因规格不符产生纠纷。",
                "商品质量", "商品,规格,重量", 5),
        new SeedFAQ("商品价格会变动吗",
                "商品价格变动说明：1.生鲜农产品价格随季节和市场供需波动属正常现象；2.促销活动期间价格优惠，活动结束后恢复原价；3.已下单商品价格锁定，不受后续调价影响；4.价格变动会在商品页显示历史价格趋势供参考；5.如遇大幅降价可联系客服申请保价（限7天内）。我们承诺价格透明，无虚假标价行为。",
                "商品质量", "价格,商品,调价,保价", 5),

        // ==================== 账号管理类（8条） ====================
        new SeedFAQ("如何注册账号",
                "注册账号非常简单：1.点击登录页面的注册按钮；2.输入手机号并获取验证码；3.设置密码并完成验证。注册后即可购物、查询订单、参与优惠活动。注册即送新人优惠券，首次下单还可享受新人专享价。一个手机号仅能注册一个账号，请妥善保管账号信息。",
                "账号管理", "注册,账号,手机号,新人", 7),
        new SeedFAQ("忘记密码怎么办",
                "找回密码流程：1.在登录页面点击忘记密码；2.输入注册手机号获取验证码；3.验证成功后设置新密码；4.新密码需包含字母和数字，长度8-20位。如手机号无法接收验证码，可联系客服人工验证身份后重置密码。建议定期修改密码并使用复杂密码提升账号安全性。修改密码后需重新登录其他设备。",
                "账号管理", "密码,登录,找回密码,手机号", 8),
        new SeedFAQ("如何登录账号",
                "登录方式说明：1.手机号+密码登录（最常用）；2.手机号+短信验证码登录（免输入密码）；3.微信一键登录（已绑定的账号）；4.第三方账号登录（支付宝等）。连续输错密码5次将锁定账号30分钟，请谨慎输入。建议在私人设备登录时勾选记住密码，公共设备请勿勾选并使用后及时退出。",
                "账号管理", "登录,密码,手机号,微信登录", 7),
        new SeedFAQ("账号被锁定怎么办",
                "账号锁定处理：1.因连续输错密码锁定，30分钟后自动解锁；2.因安全风险锁定（如异地登录），需通过手机验证码验证身份后解锁；3.如手机号变更无法验证，联系客服提交身份证明材料人工解锁；4.解锁后建议立即修改密码并检查账号安全设置；5.频繁锁定可开启二次验证提升安全性。我们锁定机制旨在保护您的账号安全。",
                "账号管理", "登录,密码,账号锁定,安全", 7),
        new SeedFAQ("支付密码怎么设置",
                "支付密码设置：1.首次使用在线支付时系统会引导设置支付密码；2.支付密码独立于登录密码，用于确认支付操作；3.设置路径：我的-安全中心-支付密码管理；4.支付密码为6位数字，请勿使用生日等简单密码；5.忘记支付密码可通过手机验证码重置。支付密码保护您的资金安全，请妥善保管，切勿泄露给他人。",
                "账号管理", "支付,密码,安全,支付密码", 7),
        new SeedFAQ("如何修改绑定手机号",
                "修改手机号流程：1.进入我的-账号设置-手机号管理；2.输入原手机号获取验证码验证身份；3.输入新手机号并获取验证码；4.验证成功后手机号即刻更换。如原手机号无法接收验证码，需联系客服进行人工审核。更换手机号后请及时更新收货地址联系方式，避免影响订单配送和售后沟通。",
                "账号管理", "注册,手机号,修改手机号,账号", 5),
        new SeedFAQ("账号安全如何保障",
                "账号安全保障措施：1.密码采用BCrypt加密存储，无法逆向破解；2.登录采用HTTPS加密传输，防止信息泄露；3.支持开启二次验证（短信验证码）提升安全性；4.异常登录会触发风控提醒；5.建议不要在公共WiFi下登录账号，不点击不明链接，不向他人透露账号密码。如发现账号异常请立即修改密码并联系客服。",
                "账号管理", "登录,密码,安全,账号保护", 6),
        new SeedFAQ("实名认证怎么做",
                "实名认证流程：1.进入我的-账号设置-实名认证；2.输入真实姓名和身份证号；3.上传身份证正反面照片；4.进行人脸识别验证；5.提交后1-2个工作日审核完成。实名认证可提升账号等级，解锁更多功能（如开店、大额交易）。商家入驻必须完成实名认证。我们严格保护您的个人信息，仅用于身份核实，不会用于其他用途。",
                "账号管理", "认证,注册,实名认证,身份验证", 6),

        // ==================== 优惠活动类（7条） ====================
        new SeedFAQ("有哪些优惠活动",
                "我们定期推出各类优惠活动：1.新人专享优惠（首单立减）；2.满减活动（满99减10、满199减30等）；3.限时秒杀（每日精选低价抢购）；4.助农专项补贴（指定农产品享补贴价）；5.节日大促（春节、618、双11等）。关注首页活动专区或订阅消息通知，获取最新优惠信息。不同活动可叠加使用，具体以活动规则为准。",
                "优惠活动", "优惠,满减,秒杀,助农,新人", 7),
        new SeedFAQ("优惠券怎么使用",
                "优惠券使用方法：1.下单结算页面会自动展示可用优惠券；2.系统默认选择最优优惠券，也可手动切换；3.优惠券需满足使用门槛（如满99元可用）；4.每笔订单仅能使用一张优惠券（特殊活动除外）；5.优惠券不可叠加现金使用。使用前请查看优惠券有效期和使用范围，过期优惠券将自动作废。部分商品不支持优惠券，详见商品页说明。",
                "优惠活动", "优惠券,订单,使用,满减", 7),
        new SeedFAQ("满减活动规则是什么",
                "满减活动规则：1.满减门槛按订单实付金额计算（不含运费）；2.如满99减10，订单商品总额需达到99元才能享受10元优惠；3.满减优惠可与优惠券叠加使用（先满减后优惠券）；4.退款时如订单金额不再满足满减门槛，需扣除已享受的优惠金额；5.不同满减活动不可叠加，以最优为准。满减活动旨在让利消费者，多多购买更划算。",
                "优惠活动", "满减,优惠,订单,规则", 7),
        new SeedFAQ("秒杀活动怎么参与",
                "秒杀活动参与方式：1.首页每日更新秒杀商品，限量超低价；2.秒杀开始前可设置开抢提醒；3.秒杀开始后点击立即抢购，先到先得；4.秒杀商品每人限购1件，防止囤积；5.秒杀订单需在15分钟内完成支付，超时自动取消。秒杀商品数量有限，抢完即止。秒杀价不支持价保，不可使用优惠券，但可享受满减优惠。",
                "优惠活动", "秒杀,优惠,抢购,限时", 7),
        new SeedFAQ("优惠券在哪里领取",
                "优惠券领取渠道：1.首页领券中心每日更新可领优惠券；2.商品详情页常驻商家优惠券；3.新人注册自动发放新人券礼包；4.参与活动（签到、分享、评价）获得奖励券；5.关注公众号不定期发放粉丝专享券；6.会员升级赠送专属优惠券。领取的优惠券在7-30天内有效，请及时使用。部分大额优惠券限量发放，先到先得。",
                "优惠活动", "优惠券,领取,新人,会员", 6),
        new SeedFAQ("优惠可以叠加使用吗",
                "优惠叠加规则：1.满减优惠与优惠券可叠加使用（如满99减10后再用10元券）；2.不同满减活动之间不可叠加，自动取最优；3.多张优惠券不可叠加，每单限用一张；4.秒杀商品不可使用优惠券，但可参与满减；5.助农补贴商品可与其他优惠叠加。结算页面会自动计算最优方案，您也可以手动调整。叠加规则以活动页面说明为准。",
                "优惠活动", "优惠,满减,优惠券,叠加", 6),
        new SeedFAQ("新人有什么优惠",
                "新人专享优惠：1.注册即送新人礼包（含多张优惠券，价值50元+）；2.首单立减10元（无门槛）；3.新人专享秒杀价（部分商品低于5折）；4.新人首单免运费；5.专属客服一对一服务。新人优惠仅限首次注册用户，每个手机号和设备仅能享受一次。注册后7天内下单均可使用新人券，超期作废，建议尽快使用。",
                "优惠活动", "优惠,优惠券,新人,注册", 7),

        // ==================== 平台介绍类（5条） ====================
        new SeedFAQ("平台是什么",
                "我们是乡村振兴农产品销售平台，致力于连接农户与消费者，提供新鲜优质的农产品直供服务。平台支持各类农产品销售，包括水果、蔬菜、粮油、肉类、蛋类等，助农惠民。通过减少中间环节，让农户卖得更好，让消费者买得更实惠。平台秉承助农初心，每笔订单都包含对农户的支持，让城市居民享受源头好货，助力乡村振兴。",
                "平台介绍", "助农,平台,乡村振兴,农产品", 5),
        new SeedFAQ("如何成为平台供应商",
                "供应商入驻流程：1.注册平台账号并完成实名认证；2.提交供应商申请，上传营业执照、食品经营许可证等资质；3.补充农产品检测报告、产地证明等材料；4.平台审核（1-3个工作日）；5.审核通过后签订合作协议并开店上架商品。我们对供应商资质严格把关，确保商品来源正规可靠。入驻咨询可联系招商客服，享有一对一指导服务。",
                "平台介绍", "供应商,认证,入驻,资质", 6),
        new SeedFAQ("平台有哪些助农活动",
                "助农活动形式多样：1.爱心助农采摘活动，可线下体验采摘乐趣并帮助农户；2.贫困地区农产品专项补贴销售；3.滞销农产品紧急助销通道；4.直播带货助农专场；5.认购认养农业（如认养果树、认养猪）。每笔助农订单我们都会公示帮扶效果，让公益透明化。关注助农专区，参与乡村振兴，让消费变得更有意义。",
                "平台介绍", "助农,采摘,直播,活动", 6),
        new SeedFAQ("平台支持直播带货吗",
                "直播带货服务：1.平台支持商家开设直播间进行农产品带货销售；2.消费者可在直播中专享直播优惠价；3.直播可直观展示商品产地、品质、加工过程；4.助农直播专场帮助偏远地区农户销售；5.关注喜欢的商家直播间，开播自动提醒。直播带货让消费者买得明白，让农户卖得好。我们也欢迎有影响力的主播合作开展助农直播专场。",
                "平台介绍", "直播,商品,助农,带货", 5),
        new SeedFAQ("农产品种植养殖信息在哪看",
                "种植养殖信息查看：1.商品详情页展示产地、种植户、种植过程等信息；2.扫码追溯二维码可查看详细的种植养殖记录；3.部分商品配有产地视频介绍；4.商家店铺页面展示农场或基地实景；5.关注商家可获取种植动态更新。我们鼓励商家透明化展示种植养殖过程，让消费者了解食材来源。有机种植、生态养殖的商品会有专门标识，方便识别选择。",
                "平台介绍", "种植,养殖,农产品,追溯", 5)
    );

    /** 核心业务场景关键词（用于计算覆盖率） */
    private static final Set<String> CORE_BUSINESS_KEYWORDS = new HashSet<>(Arrays.asList(
        "退换货", "退款", "配送", "物流", "质量", "注册", "登录", "密码",
        "优惠", "满减", "秒杀", "优惠券", "订单", "支付", "发票",
        "售后", "保修", "投诉", "商品", "价格", "库存", "上架",
        "供应商", "认证", "助农", "直播", "采摘", "种植", "养殖"
    ));

    @PostConstruct
    public void init() {
        try {
            if (faqMapper == null) {
                logger.warn("KnowledgeFaqMapper 未注入，跳过种子FAQ初始化");
                return;
            }
            Long existingCount = faqMapper.selectCount(null);
            if (existingCount == null) {
                existingCount = 0L;
            }
            logger.info("种子FAQ初始化检查：当前FAQ数量={}", existingCount);
            if (existingCount == 0L) {
                logger.info("知识库为空，开始执行冷启动种子FAQ初始化...");
                initializeSeedFAQs();
            } else {
                logger.info("知识库已有FAQ数据，跳过种子数据初始化");
                // 即使不初始化也输出覆盖率监控信息
                try {
                    double coverageRate = calculateCoverageRate();
                    logger.info("当前知识库核心业务覆盖率: {} (阈值: {})", String.format("%.2f%%", coverageRate * 100), COVERAGE_ALERT_THRESHOLD);
                    if (coverageRate < COVERAGE_ALERT_THRESHOLD) {
                        logger.warn("知识库覆盖率低于告警阈值，建议补充种子FAQ数据");
                    }
                } catch (Exception e) {
                    logger.warn("计算覆盖率失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            // 异常时记录日志但不阻止启动
            logger.error("种子FAQ初始化异常，不阻止应用启动: {}", e.getMessage(), e);
        }
    }

    /**
     * 初始化种子FAQ数据
     */
    public void initializeSeedFAQs() {
        if (faqMapper == null) {
            logger.warn("KnowledgeFaqMapper 未注入，无法初始化种子FAQ");
            return;
        }
        int successCount = 0;
        int failCount = 0;
        logger.info("开始批量初始化种子FAQ数据，共 {} 条", SEED_FAQS.size());
        for (SeedFAQ seed : SEED_FAQS) {
            try {
                KnowledgeFaq faq = new KnowledgeFaq();
                faq.setQuestion(seed.question);
                faq.setAnswer(seed.answer);
                faq.setCategory(seed.category);
                faq.setKeywords(seed.keywords);
                faq.setPriority(seed.priority);
                faq.setHitCount(0);
                faq.setStatus(1);
                faq.setEmbeddingModel(embeddingService != null ? embeddingService.getModelName() : "unknown");
                faq.setCreatedBy(1L);
                faq.setCreatedAt(LocalDateTime.now());
                faq.setUpdatedAt(LocalDateTime.now());

                int rows = faqMapper.insert(faq);
                if (rows <= 0 || faq.getId() == null) {
                    logger.warn("插入种子FAQ失败（影响行数为0或无ID）: question={}", seed.question);
                    failCount++;
                    continue;
                }

                // 生成向量并存储
                if (embeddingService != null && vectorStoreService != null) {
                    try {
                        float[] vector = embeddingService.embed(seed.question);
                        vectorStoreService.storeFaqEmbedding(faq.getId(), seed.question, vector);
                    } catch (Exception e) {
                        logger.warn("种子FAQ向量生成/存储失败，但FAQ记录已入库: faqId={}, error={}", faq.getId(), e.getMessage());
                    }
                } else {
                    logger.warn("EmbeddingService或VectorStoreService未注入，跳过向量生成: faqId={}", faq.getId());
                }

                successCount++;
                logger.debug("种子FAQ初始化成功: id={}, question={}", faq.getId(), seed.question);
            } catch (Exception e) {
                failCount++;
                logger.error("种子FAQ初始化失败: question={}, error={}", seed.question, e.getMessage(), e);
            }
        }
        logger.info("种子FAQ初始化完成: 成功 {} 条, 失败 {} 条, 共 {} 条", successCount, failCount, SEED_FAQS.size());

        // 初始化完成后输出覆盖率
        try {
            double coverageRate = calculateCoverageRate();
            logger.info("种子FAQ初始化后知识库覆盖率: {}", String.format("%.2f%%", coverageRate * 100));
        } catch (Exception e) {
            logger.warn("初始化后计算覆盖率失败: {}", e.getMessage());
        }
    }

    /**
     * 计算知识库覆盖率
     * 覆盖率 = 已覆盖的核心关键词数 / 总核心关键词数
     */
    public double calculateCoverageRate() {
        if (faqMapper == null) {
            return 0.0;
        }
        List<KnowledgeFaq> faqs;
        try {
            faqs = faqMapper.selectAllEnabled();
        } catch (Exception e) {
            logger.warn("查询FAQ失败，尝试使用selectList: {}", e.getMessage());
            try {
                faqs = faqMapper.selectList(null);
            } catch (Exception ex) {
                logger.error("查询FAQ失败: {}", ex.getMessage());
                return 0.0;
            }
        }
        if (faqs == null || faqs.isEmpty() || CORE_BUSINESS_KEYWORDS.isEmpty()) {
            return 0.0;
        }

        Set<String> coveredKeywords = new HashSet<>();
        for (KnowledgeFaq faq : faqs) {
            String text = buildFaqSearchText(faq);
            for (String keyword : CORE_BUSINESS_KEYWORDS) {
                if (text.contains(keyword)) {
                    coveredKeywords.add(keyword);
                }
            }
        }
        return (double) coveredKeywords.size() / (double) CORE_BUSINESS_KEYWORDS.size();
    }

    /**
     * 获取覆盖率详情
     */
    public Map<String, Object> getCoverageDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        if (faqMapper == null) {
            details.put("coverageRate", 0.0);
            details.put("totalKeywords", CORE_BUSINESS_KEYWORDS.size());
            details.put("coveredCount", 0);
            details.put("uncoveredCount", CORE_BUSINESS_KEYWORDS.size());
            details.put("coveredKeywords", new ArrayList<String>());
            details.put("uncoveredKeywords", new ArrayList<>(CORE_BUSINESS_KEYWORDS));
            details.put("needAlert", true);
            details.put("alertMessage", "KnowledgeFaqMapper未注入，无法计算覆盖率");
            return details;
        }

        List<KnowledgeFaq> faqs;
        try {
            faqs = faqMapper.selectAllEnabled();
        } catch (Exception e) {
            logger.warn("查询FAQ失败，尝试使用selectList: {}", e.getMessage());
            try {
                faqs = faqMapper.selectList(null);
            } catch (Exception ex) {
                faqs = new ArrayList<>();
            }
        }
        if (faqs == null) {
            faqs = new ArrayList<>();
        }

        Set<String> coveredKeywords = new HashSet<>();
        for (KnowledgeFaq faq : faqs) {
            String text = buildFaqSearchText(faq);
            for (String keyword : CORE_BUSINESS_KEYWORDS) {
                if (text.contains(keyword)) {
                    coveredKeywords.add(keyword);
                }
            }
        }

        List<String> coveredList = new ArrayList<>(coveredKeywords);
        List<String> uncoveredList = new ArrayList<>();
        for (String keyword : CORE_BUSINESS_KEYWORDS) {
            if (!coveredKeywords.contains(keyword)) {
                uncoveredList.add(keyword);
            }
        }

        double coverageRate = (double) coveredKeywords.size() / (double) CORE_BUSINESS_KEYWORDS.size();
        boolean needAlert = coverageRate < COVERAGE_ALERT_THRESHOLD;

        details.put("coverageRate", coverageRate);
        details.put("totalKeywords", CORE_BUSINESS_KEYWORDS.size());
        details.put("coveredCount", coveredKeywords.size());
        details.put("uncoveredCount", uncoveredList.size());
        details.put("coveredKeywords", coveredList);
        details.put("uncoveredKeywords", uncoveredList);
        details.put("needAlert", needAlert);
        details.put("faqCount", faqs.size());
        details.put("alertThreshold", COVERAGE_ALERT_THRESHOLD);

        if (needAlert) {
            String alertMessage = String.format(
                    "知识库覆盖率仅为 %.2f%%（低于阈值 %.2f%%），存在 %d 个核心业务关键词未覆盖: %s。建议补充对应类别的种子FAQ数据以提升问答覆盖率。",
                    coverageRate * 100, COVERAGE_ALERT_THRESHOLD * 100,
                    uncoveredList.size(), uncoveredList.toString());
            details.put("alertMessage", alertMessage);
            logger.warn(alertMessage);
        }

        return details;
    }

    /**
     * 多级查询匹配：精确关键词匹配 → 向量检索 → LLM生成
     * 返回匹配级别：1=精确匹配, 2=向量匹配, 3=需要LLM
     */
    public int getMatchLevel(String query) {
        if (query == null || query.trim().isEmpty()) {
            return 3;
        }
        if (faqMapper == null) {
            return 3;
        }

        List<KnowledgeFaq> faqs;
        try {
            faqs = faqMapper.selectAllEnabled();
        } catch (Exception e) {
            logger.warn("查询FAQ失败，尝试使用selectList: {}", e.getMessage());
            try {
                faqs = faqMapper.selectList(null);
            } catch (Exception ex) {
                logger.error("查询FAQ失败: {}", ex.getMessage());
                return 3;
            }
        }
        if (faqs == null || faqs.isEmpty()) {
            return 3;
        }

        String normalizedQuery = query.trim();
        // 1. 精确关键词匹配：检查query是否完全等于或包含某FAQ问题
        for (KnowledgeFaq faq : faqs) {
            String question = faq.getQuestion();
            if (question == null || question.isEmpty()) {
                continue;
            }
            // 完全相等
            if (normalizedQuery.equals(question.trim())) {
                return 1;
            }
            // query 包含 FAQ 问题 或 FAQ 问题包含 query
            if (normalizedQuery.contains(question.trim()) || question.trim().contains(normalizedQuery)) {
                return 1;
            }
        }
        // 2. 否则返回3（需要向量检索或LLM）
        return 3;
    }

    /**
     * 拼接FAQ的检索文本（question + keywords + answer）
     */
    private String buildFaqSearchText(KnowledgeFaq faq) {
        StringBuilder sb = new StringBuilder();
        if (faq.getQuestion() != null) {
            sb.append(faq.getQuestion());
        }
        sb.append(" ");
        if (faq.getKeywords() != null) {
            sb.append(faq.getKeywords());
        }
        sb.append(" ");
        if (faq.getAnswer() != null) {
            sb.append(faq.getAnswer());
        }
        return sb.toString();
    }

    private static class SeedFAQ {
        final String question;
        final String answer;
        final String category;
        final String keywords;
        final int priority;

        SeedFAQ(String question, String answer, String category, String keywords, int priority) {
            this.question = question;
            this.answer = answer;
            this.category = category;
            this.keywords = keywords;
            this.priority = priority;
        }
    }
}
