package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.DivinationRecord;
import cn.edu.sdu.java.server.models.Person;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.DivinationRecordRepository;
import cn.edu.sdu.java.server.repositorys.UserRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import cn.edu.sdu.java.server.util.DateTimeTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DivinationService {
    private final DivinationRecordRepository divinationRecordRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final List<Trigram> TRIGRAMS = List.of(
            new Trigram("乾", "天", "金", "刚健、主动、开端、规则", new int[]{1, 1, 1}),
            new Trigram("兑", "泽", "金", "表达、喜悦、沟通、缺口", new int[]{1, 1, 0}),
            new Trigram("离", "火", "火", "明晰、展示、依附、文采", new int[]{1, 0, 1}),
            new Trigram("震", "雷", "木", "启动、震动、行动、突破", new int[]{1, 0, 0}),
            new Trigram("巽", "风", "木", "进入、传播、调整、渐进", new int[]{0, 1, 1}),
            new Trigram("坎", "水", "水", "风险、流动、思考、压力", new int[]{0, 1, 0}),
            new Trigram("艮", "山", "土", "停止、边界、积累、沉稳", new int[]{0, 0, 1}),
            new Trigram("坤", "地", "土", "承载、配合、落实、资源", new int[]{0, 0, 0})
    );

    private static final String[][] HEXAGRAM_NAMES = {
            {"乾为天", "泽天夬", "火天大有", "雷天大壮", "风天小畜", "水天需", "山天大畜", "地天泰"},
            {"天泽履", "兑为泽", "火泽睽", "雷泽归妹", "风泽中孚", "水泽节", "山泽损", "地泽临"},
            {"天火同人", "泽火革", "离为火", "雷火丰", "风火家人", "水火既济", "山火贲", "地火明夷"},
            {"天雷无妄", "泽雷随", "火雷噬嗑", "震为雷", "风雷益", "水雷屯", "山雷颐", "地雷复"},
            {"天风姤", "泽风大过", "火风鼎", "雷风恒", "巽为风", "水风井", "山风蛊", "地风升"},
            {"天水讼", "泽水困", "火水未济", "雷水解", "风水涣", "坎为水", "山水蒙", "地水师"},
            {"天山遁", "泽山咸", "火山旅", "雷山小过", "风山渐", "水山蹇", "艮为山", "地山谦"},
            {"天地否", "泽地萃", "火地晋", "雷地豫", "风地观", "水地比", "山地剥", "坤为地"}
    };

    private static final List<TarotCard> TAROT_CARDS = List.of(
            new TarotCard("愚者", "新的开始、自由、探索", "鲁莽、逃避、缺少计划"),
            new TarotCard("魔术师", "资源整合、表达、创造", "能力分散、承诺过多、技巧未熟"),
            new TarotCard("女祭司", "直觉、沉静、隐藏信息", "信息不透明、过度犹豫、压抑感受"),
            new TarotCard("皇后", "滋养、成果、审美", "依赖舒适区、情绪化、拖延产出"),
            new TarotCard("皇帝", "秩序、结构、掌控", "控制过度、僵化、压力"),
            new TarotCard("教皇", "传统、学习、规则", "形式主义、被标准束缚、缺少个人理解"),
            new TarotCard("恋人", "选择、连接、价值一致", "摇摆、关系失衡、选择焦虑"),
            new TarotCard("战车", "推进、意志、胜利", "方向混乱、急躁、消耗"),
            new TarotCard("力量", "耐心、内在力量、温柔坚持", "信心不足、情绪失控、强撑"),
            new TarotCard("隐士", "研究、独处、深度思考", "孤立、过度内耗、缺少反馈"),
            new TarotCard("命运之轮", "周期变化、机会、转折", "被动等待、节奏失衡、不可控"),
            new TarotCard("正义", "判断、平衡、责任", "偏见、失衡、没有承担后果"),
            new TarotCard("倒吊人", "换角度、暂停、牺牲", "停滞、无效忍耐、看不清重点"),
            new TarotCard("死神", "结束、转化、更新", "抗拒变化、旧模式拖累"),
            new TarotCard("节制", "调和、节奏、协作", "过度折中、拖慢进度、边界不清"),
            new TarotCard("恶魔", "欲望、束缚、执念", "依赖、沉迷、短期诱惑"),
            new TarotCard("高塔", "突发、破局、真相显露", "逃避问题、基础不稳"),
            new TarotCard("星星", "希望、疗愈、长期愿景", "期待过高、行动不足"),
            new TarotCard("月亮", "不确定、潜意识、迷雾", "误判、焦虑、信息混杂"),
            new TarotCard("太阳", "清晰、成功、公开表达", "过度乐观、忽略细节"),
            new TarotCard("审判", "复盘、觉醒、重新选择", "害怕评价、拖延决定"),
            new TarotCard("世界", "完成、整合、阶段成果", "收尾不足、目标过大")
    );

    public DivinationService(DivinationRecordRepository divinationRecordRepository,
                             UserRepository userRepository,
                             ObjectMapper objectMapper) {
        this.divinationRecordRepository = divinationRecordRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public DataResponse plumBlossom(DataRequest request) {
        String question = defaultString(request.getString("question"), "我现在关心的问题");
        String background = defaultString(request.getString("background"), "暂无详细背景");
        String method = defaultString(request.getString("method"), "当前时间起卦");
        String input = defaultString(request.getString("input"), question + background);
        boolean includeStudy = request.getBoolean("includeStudy");

        int seed = buildSeed(method, input, question, background);
        Trigram upper = TRIGRAMS.get(Math.floorMod(seed, 8));
        Trigram lower = TRIGRAMS.get(Math.floorMod(seed / 8 + question.length(), 8));
        int movingLine = Math.floorMod(seed + background.length(), 6) + 1;

        Hexagram base = hexagram(upper, lower);
        Hexagram mutual = mutualHexagram(base.lines());
        Hexagram changed = changedHexagram(base.lines(), movingLine);
        Trigram body = movingLine <= 3 ? upper : lower;
        Trigram use = movingLine <= 3 ? lower : upper;
        String relation = elementRelation(body.element(), use.element());
        String report = buildPlumReport(question, background, method, input, base, mutual, changed, movingLine, body, use, relation, includeStudy);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "PLUM_BLOSSOM");
        result.put("question", question);
        result.put("background", background);
        result.put("method", method);
        result.put("baseHexagram", base.name());
        result.put("mutualHexagram", mutual.name());
        result.put("changedHexagram", changed.name());
        result.put("movingLine", movingLine);
        result.put("bodyTrigram", body.name());
        result.put("useTrigram", use.name());
        result.put("relation", relation);
        result.put("reportText", report);
        result.put("recordId", saveRecord("PLUM_BLOSSOM", question, background, method, mapOf("input", input, "includeStudy", includeStudy), result, report).getRecordId());
        return CommonMethod.getReturnData(result, "梅花易数报告生成成功。");
    }

    public DataResponse tarot(DataRequest request) {
        String question = defaultString(request.getString("question"), "我现在关心的问题");
        String background = defaultString(request.getString("background"), "暂无详细背景");
        String spread = defaultString(request.getString("spread"), "三张牌");
        List<String> positions = tarotPositions(spread);
        List<Map<String, Object>> cards = drawTarotCards(question, background, positions);
        String report = buildTarotReport(question, background, spread, cards);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "TAROT");
        result.put("question", question);
        result.put("background", background);
        result.put("spread", spread);
        result.put("cards", cards);
        result.put("reportText", report);
        result.put("recordId", saveRecord("TAROT", question, background, spread, mapOf("spread", spread), result, report).getRecordId());
        return CommonMethod.getReturnData(result, "塔罗指南报告生成成功。");
    }

    public DataResponse getHistoryList(DataRequest request) {
        Integer personId = CommonMethod.getPersonId();
        if (personId == null) {
            return CommonMethod.getReturnData(List.of());
        }
        List<Map<String, Object>> list = divinationRecordRepository.findByUserPersonIdOrderByRecordIdDesc(personId)
                .stream()
                .map(this::recordSummary)
                .toList();
        return CommonMethod.getReturnData(list);
    }

    public DataResponse getHistoryDetail(DataRequest request) {
        Integer recordId = request.getInteger("recordId");
        Integer personId = CommonMethod.getPersonId();
        if (recordId == null || personId == null) {
            return CommonMethod.getReturnMessageError("请选择历史报告。");
        }
        return divinationRecordRepository.findById(recordId)
                .filter(record -> record.getUser() != null && Objects.equals(record.getUser().getPersonId(), personId))
                .map(record -> CommonMethod.getReturnData(recordDetail(record)))
                .orElseGet(() -> CommonMethod.getReturnMessageError("未找到当前用户的历史报告。"));
    }

    public DataResponse deleteHistory(DataRequest request) {
        Integer recordId = request.getInteger("recordId");
        Integer personId = CommonMethod.getPersonId();
        if (recordId == null || personId == null) {
            return CommonMethod.getReturnMessageError("请选择要删除的历史报告。");
        }
        Optional<DivinationRecord> optional = divinationRecordRepository.findById(recordId)
                .filter(record -> record.getUser() != null && Objects.equals(record.getUser().getPersonId(), personId));
        if (optional.isEmpty()) {
            return CommonMethod.getReturnMessageError("未找到当前用户的历史报告。");
        }
        divinationRecordRepository.delete(optional.get());
        return CommonMethod.getReturnMessageOK("历史报告已删除。");
    }

    private DivinationRecord saveRecord(String type, String question, String background, String method,
                                        Map<String, Object> input, Map<String, Object> result, String report) {
        DivinationRecord record = new DivinationRecord();
        currentPerson().ifPresent(person -> {
            record.setUser(person);
            record.setUserName(defaultString(person.getName(), person.getNum()));
        });
        record.setType(type);
        record.setQuestion(limit(question, 200));
        record.setBackground(limit(background, 2000));
        record.setMethod(method);
        record.setInputJson(toJson(input));
        record.setResultJson(toJson(result));
        record.setReportText(report);
        record.setCreateTime(DateTimeTool.parseDateTime(new Date()));
        return divinationRecordRepository.save(record);
    }

    private String buildPlumReport(String question, String background, String method, String input,
                                   Hexagram base, Hexagram mutual, Hexagram changed, int movingLine,
                                   Trigram body, Trigram use, String relation, boolean includeStudy) {
        StringBuilder builder = new StringBuilder();
        builder.append("【梅花易数指南报告】\n\n")
                .append("一、占问信息\n")
                .append("占问主题：").append(question).append("\n")
                .append("背景描述：").append(background).append("\n")
                .append("起卦方式：").append(method).append("\n")
                .append("输入材料：").append(input).append("\n")
                .append("说明：本报告用于周易文化学习、自我观察和思路整理，不作为绝对预测或重大决策依据。\n\n")
                .append("二、起卦过程\n")
                .append("系统根据起卦方式、输入文字、当前时间和问题背景生成象数种子，再取上卦、下卦和动爻。\n")
                .append("上卦为").append(base.upper().name()).append("，象为").append(base.upper().image()).append("，五行为").append(base.upper().element()).append("；")
                .append("下卦为").append(base.lower().name()).append("，象为").append(base.lower().image()).append("，五行为").append(base.lower().element()).append("。\n")
                .append("动爻为第 ").append(movingLine).append(" 爻，提示事情中最容易发生变化的位置。\n\n")
                .append("三、本卦分析：").append(base.name()).append("\n")
                .append(base.name()).append("代表问题的当前局面。")
                .append(base.upper().meaning()).append("在外，").append(base.lower().meaning()).append("在内，说明此事既有外部环境的牵引，也有内在状态的支撑。\n")
                .append("就“").append(question).append("”而言，本卦提示你先看清当前资源、限制和主次关系，不宜只凭一时情绪推进。\n\n")
                .append("四、互卦分析：").append(mutual.name()).append("\n")
                .append("互卦取中间爻象，常用于观察事情内部结构和隐藏逻辑。")
                .append(mutual.upper().meaning()).append("与").append(mutual.lower().meaning()).append("组合，提示真正影响结果的因素可能不是表面问题，而是节奏、表达和准备程度。\n\n")
                .append("五、变卦分析：").append(changed.name()).append("\n")
                .append("变卦代表动爻变化后的趋势。")
                .append(changed.name()).append("提示后续会出现新的理解角度。若你能主动调整方法，问题会从模糊状态转向更可操作的阶段。\n\n")
                .append("六、体用关系\n")
                .append("体卦：").append(body.name()).append("（").append(body.element()).append("），代表自身立场与核心需求。\n")
                .append("用卦：").append(use.name()).append("（").append(use.element()).append("），代表外部环境、对象或需要面对的条件。\n")
                .append("五行关系：").append(relation).append("\n")
                .append("体用关系提示：如果体受用克，应先补足准备；如果体生用，容易付出较多；如果用生体，则外部条件有助力；如果比和，则适合稳步推进。\n\n")
                .append("七、动爻提示\n")
                .append("第 ").append(movingLine).append(" 爻强调此事的变化点在")
                .append(movingLine <= 2 ? "基础准备和信息收集。" : movingLine <= 4 ? "执行过程和沟通协调。" : "结果呈现和后续收束。").append("\n")
                .append("建议你把注意力放在可调整的部分，避免把精力耗在不可控因素上。\n\n")
                .append("八、综合判断\n")
                .append("整体来看，这个问题不是单纯的吉凶判断，而是一个“如何把想法落地”的问题。卦象提示你需要把目标拆清楚，先确认主题，再安排材料，最后用案例或演示增强说服力。\n\n")
                .append("九、行动建议\n")
                .append("1. 先写出问题的核心目标，避免同时追求太多方向。\n")
                .append("2. 准备一个具体案例，用案例承接理论，会比纯概念说明更清晰。\n")
                .append("3. 保留一部分反思内容，说明此报告只是象意分析和学习参考。\n")
                .append("4. 如果用于课程展示，可以按“起卦方法、卦象解释、现实建议、学习体会”四段组织。\n\n");
        if (includeStudy) {
            builder.append("十、学习笔记\n")
                    .append("本卦看当前处境，互卦看内部结构，变卦看变化方向；体卦代表自己，用卦代表外部对象。梅花易数的价值不在于替人做决定，而在于用象数框架帮助人重新组织问题。\n\n");
        }
        builder.append("十一、参考边界\n")
                .append("本报告仅用于传统文化学习、课程讨论和自我反思。涉及医疗、法律、财务、考试录取、关系重大决定等事项，应以现实证据、专业意见和个人负责的判断为准。");
        return builder.toString();
    }

    private String buildTarotReport(String question, String background, String spread, List<Map<String, Object>> cards) {
        StringBuilder builder = new StringBuilder();
        builder.append("【塔罗牌阵指南报告】\n\n")
                .append("一、问题背景\n")
                .append("问题主题：").append(question).append("\n")
                .append("背景描述：").append(background).append("\n")
                .append("牌阵类型：").append(spread).append("\n")
                .append("说明：塔罗报告用于象征分析、自我提问和行动整理，不作为绝对预测。\n\n")
                .append("二、牌阵说明\n")
                .append(spread).append("适合从多个角度观察同一个问题。每张牌的位置含义比单独牌义更重要，应结合你的问题背景一起看。\n\n")
                .append("三、抽牌结果\n");
        for (Map<String, Object> card : cards) {
            builder.append("- ").append(card.get("position")).append("：")
                    .append(card.get("name")).append("（").append(card.get("orientation")).append("）\n");
        }
        builder.append("\n四、逐牌解释\n");
        for (Map<String, Object> card : cards) {
            builder.append(card.get("position")).append("：").append(card.get("name")).append("（").append(card.get("orientation")).append("）\n")
                    .append("关键词：").append(card.get("keywords")).append("\n")
                    .append("解释：这张牌在当前位置提示你关注“").append(card.get("focus")).append("”。")
                    .append(card.get("advice")).append("\n\n");
        }
        builder.append("五、牌阵综合分析\n")
                .append("这组牌的重点不是给出单一答案，而是把问题拆成状态、阻碍、资源和行动。")
                .append("如果多张牌都指向准备、秩序或沟通，说明你需要先整理结构；如果多张牌指向变化或不确定，则说明现在最重要的是降低风险并保留弹性。\n\n")
                .append("六、当前状态\n")
                .append("你现在的问题意识已经比较明确，但仍需要把背景材料、现实条件和个人期待分开。不要急于得到“好或坏”的判断，先问自己：我真正能控制的部分是什么？\n\n")
                .append("七、潜在变化\n")
                .append("后续变化通常来自两个方向：一是信息变得更清楚，二是你采取了新的行动。牌阵鼓励你用小步骤验证判断，而不是一次性把所有问题压到最终结果上。\n\n")
                .append("八、行动建议\n")
                .append("1. 把问题写成一个可以行动的目标。\n")
                .append("2. 找出当前最缺的一类资源：信息、时间、信心、协作或表达。\n")
                .append("3. 选择一件 24 小时内可以完成的小事，让问题开始移动。\n")
                .append("4. 保留复盘记录，观察一周后自己的状态是否发生变化。\n\n")
                .append("九、提醒与边界\n")
                .append("塔罗牌不能替代现实判断。它更像一面镜子，帮助你看见自己对问题的理解、期待和盲区。\n\n")
                .append("十、总结句\n")
                .append("真正重要的不是牌替你决定什么，而是你借由牌面看见了下一步可以怎样走。");
        return builder.toString();
    }

    private int buildSeed(String method, String input, String question, String background) {
        if (method.contains("数字")) {
            String digits = input.replaceAll("\\D", "");
            if (!digits.isBlank()) {
                try {
                    return Math.abs(Integer.parseInt(digits.substring(0, Math.min(8, digits.length()))));
                } catch (NumberFormatException ignored) {
                    return digits.chars().sum();
                }
            }
        }
        if (method.contains("时间")) {
            LocalDateTime now = LocalDateTime.now();
            return now.getYear() + now.getMonthValue() * 13 + now.getDayOfMonth() * 17 + now.getHour() * 19 + now.getMinute() * 23;
        }
        return Math.abs((input + question + background).chars().sum());
    }

    private Hexagram hexagram(Trigram upper, Trigram lower) {
        return new Hexagram(HEXAGRAM_NAMES[indexOf(upper)][indexOf(lower)], upper, lower);
    }

    private Hexagram mutualHexagram(int[] lines) {
        Trigram lower = trigramByLines(new int[]{lines[1], lines[2], lines[3]});
        Trigram upper = trigramByLines(new int[]{lines[2], lines[3], lines[4]});
        return hexagram(upper, lower);
    }

    private Hexagram changedHexagram(int[] lines, int movingLine) {
        int[] changed = Arrays.copyOf(lines, lines.length);
        changed[movingLine - 1] = changed[movingLine - 1] == 1 ? 0 : 1;
        Trigram lower = trigramByLines(new int[]{changed[0], changed[1], changed[2]});
        Trigram upper = trigramByLines(new int[]{changed[3], changed[4], changed[5]});
        return hexagram(upper, lower);
    }

    private Trigram trigramByLines(int[] lines) {
        for (Trigram trigram : TRIGRAMS) {
            if (Arrays.equals(trigram.lines(), lines)) {
                return trigram;
            }
        }
        return TRIGRAMS.get(0);
    }

    private int indexOf(Trigram trigram) {
        return TRIGRAMS.indexOf(trigram);
    }

    private String elementRelation(String body, String use) {
        if (Objects.equals(body, use)) {
            return "体用比和：自身状态与外部条件较一致，适合稳步推进。";
        }
        if (generates(body, use)) {
            return "体生用：你需要主动付出、表达或投入，过程较耗力，但有利于推动局面。";
        }
        if (generates(use, body)) {
            return "用生体：外部条件对你有帮助，适合借助资源、老师、同伴或已有资料。";
        }
        if (controls(body, use)) {
            return "体克用：你有能力处理问题，但要避免过度用力或急于控制结果。";
        }
        return "用克体：外部压力较明显，建议先补足准备、降低风险，再推进关键动作。";
    }

    private boolean generates(String a, String b) {
        return ("木".equals(a) && "火".equals(b)) || ("火".equals(a) && "土".equals(b))
                || ("土".equals(a) && "金".equals(b)) || ("金".equals(a) && "水".equals(b))
                || ("水".equals(a) && "木".equals(b));
    }

    private boolean controls(String a, String b) {
        return ("木".equals(a) && "土".equals(b)) || ("土".equals(a) && "水".equals(b))
                || ("水".equals(a) && "火".equals(b)) || ("火".equals(a) && "金".equals(b))
                || ("金".equals(a) && "木".equals(b));
    }

    private List<String> tarotPositions(String spread) {
        return switch (spread) {
            case "单张牌" -> List.of("核心指引");
            case "四元素牌阵" -> List.of("现实层面", "情绪层面", "思考层面", "行动层面");
            case "十字牌阵" -> List.of("当前状态", "主要阻碍", "内在动机", "外部影响", "过去基础", "近期趋势", "可用资源", "行动建议", "需要警惕", "综合结果");
            default -> List.of("过去基础", "现在状态", "未来趋势");
        };
    }

    private List<Map<String, Object>> drawTarotCards(String question, String background, List<String> positions) {
        List<TarotCard> deck = new ArrayList<>(TAROT_CARDS);
        Collections.shuffle(deck, new Random(System.nanoTime() + question.hashCode() + background.hashCode()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
            TarotCard card = deck.get(i % deck.size());
            boolean reversed = ThreadLocalRandom.current().nextBoolean();
            String keywords = reversed ? card.reversed() : card.upright();
            result.add(mapOf(
                    "position", positions.get(i),
                    "name", card.name(),
                    "orientation", reversed ? "逆位" : "正位",
                    "keywords", keywords,
                    "focus", positions.get(i) + "中的" + keywords.split("、")[0],
                    "advice", tarotAdvice(positions.get(i), reversed)
            ));
        }
        return result;
    }

    private String tarotAdvice(String position, boolean reversed) {
        if (reversed) {
            return "建议先检查这里是否存在误解、拖延或过度用力，不急着推进，先把卡住的原因说清楚。";
        }
        return "建议顺着这个方向寻找可执行的小步骤，让它从想法变成具体行动。";
    }

    private Optional<Person> currentPerson() {
        String username = CommonMethod.getUsername();
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUserName(username).map(user -> user.getPerson());
    }

    private Map<String, Object> recordSummary(DivinationRecord record) {
        return mapOf(
                "recordId", record.getRecordId(),
                "type", record.getType(),
                "typeName", "PLUM_BLOSSOM".equals(record.getType()) ? "梅花易数" : "塔罗牌阵",
                "question", record.getQuestion(),
                "method", record.getMethod(),
                "createTime", record.getCreateTime()
        );
    }

    private Map<String, Object> recordDetail(DivinationRecord record) {
        Map<String, Object> data = recordSummary(record);
        data.put("background", record.getBackground());
        data.put("inputJson", record.getInputJson());
        data.put("resultJson", record.getResultJson());
        data.put("reportText", record.getReportText());
        return data;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String limit(String value, int max) {
        String text = defaultString(value, "");
        return text.length() <= max ? text : text.substring(0, max);
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private record Trigram(String name, String image, String element, String meaning, int[] lines) {
    }

    private record Hexagram(String name, Trigram upper, Trigram lower) {
        private int[] lines() {
            return new int[]{lower.lines()[0], lower.lines()[1], lower.lines()[2], upper.lines()[0], upper.lines()[1], upper.lines()[2]};
        }
    }

    private record TarotCard(String name, String upright, String reversed) {
    }
}
