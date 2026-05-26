package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.CommunityComment;
import cn.edu.sdu.java.server.models.CommunityPost;
import cn.edu.sdu.java.server.models.Person;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.OptionItem;
import cn.edu.sdu.java.server.payload.response.OptionItemList;
import cn.edu.sdu.java.server.repositorys.CommunityCommentRepository;
import cn.edu.sdu.java.server.repositorys.CommunityPostRepository;
import cn.edu.sdu.java.server.repositorys.DictionaryInfoRepository;
import cn.edu.sdu.java.server.repositorys.PersonRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class CommunityService {
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
    private static final String CATEGORY_CODE = "COMMUNITY_CATEGORY";
    private static final List<String> DEFAULT_CATEGORY_LIST = List.of("综合交流", "课程讨论", "校园互助", "学习分享");

    private final CommunityPostRepository communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final PersonRepository personRepository;
    private final DictionaryInfoRepository dictionaryInfoRepository;

    public CommunityService(
            CommunityPostRepository communityPostRepository,
            CommunityCommentRepository communityCommentRepository,
            PersonRepository personRepository,
            DictionaryInfoRepository dictionaryInfoRepository
    ) {
        this.communityPostRepository = communityPostRepository;
        this.communityCommentRepository = communityCommentRepository;
        this.personRepository = personRepository;
        this.dictionaryInfoRepository = dictionaryInfoRepository;
    }

    public DataResponse getPostList(DataRequest dataRequest) {
        ensureDefaultCategories();
        String category = normalize(dataRequest.getString("category"));
        String keyword = normalize(dataRequest.getString("keyword"));
        Boolean onlyMine = dataRequest.getBoolean("onlyMine");
        List<CommunityPost> allPosts = communityPostRepository.findAllByOrderByUpdatedTimeDescCreatedTimeDesc();
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (CommunityPost post : allPosts) {
            if (!matchesFilter(post, category, keyword, Boolean.TRUE.equals(onlyMine))) {
                continue;
            }
            dataList.add(getPostSummaryMap(post));
        }
        return CommonMethod.getReturnData(dataList);
    }

    public OptionItemList getCategoryOptionList(DataRequest dataRequest) {
        ensureDefaultCategories();
        Set<String> categorySet = new LinkedHashSet<>();
        categorySet.addAll(DEFAULT_CATEGORY_LIST);
        dictionaryInfoRepository.getDictionaryInfoList(CATEGORY_CODE)
                .forEach(item -> categorySet.add(normalize(item.getLabel()).isEmpty() ? normalize(item.getValue()) : normalize(item.getLabel())));
        List<OptionItem> itemList = new ArrayList<>();
        int index = 1;
        for (String category : categorySet) {
            if (!category.isEmpty()) {
                itemList.add(new OptionItem(index++, category, category));
            }
        }
        return new OptionItemList(0, itemList);
    }

    public DataResponse getPostDetail(DataRequest dataRequest) {
        Integer postId = dataRequest.getInteger("communityPostId");
        if (postId == null || postId <= 0) {
            return CommonMethod.getReturnMessageError("帖子不存在！");
        }
        Optional<CommunityPost> op = communityPostRepository.findById(postId);
        if (op.isEmpty()) {
            return CommonMethod.getReturnMessageError("帖子不存在！");
        }
        CommunityPost post = op.get();
        Map<String, Object> data = new HashMap<>();
        data.put("post", getPostDetailMap(post));
        data.put("comments", getCommentMapList(post.getCommunityPostId()));
        return CommonMethod.getReturnData(data);
    }

    @Transactional
    public DataResponse postSave(DataRequest dataRequest) {
        Integer postId = dataRequest.getInteger("communityPostId");
        String title = normalize(dataRequest.getString("title"));
        String category = normalize(dataRequest.getString("category"));
        String content = normalize(dataRequest.getString("content"));
        String mediaType = normalize(dataRequest.getString("mediaType"));
        String mediaUrl = normalize(dataRequest.getString("mediaUrl"));
        String linkUrl = normalize(dataRequest.getString("linkUrl"));
        if (title.isEmpty()) {
            return CommonMethod.getReturnMessageError("请输入帖子标题！");
        }
        if (category.isEmpty()) {
            return CommonMethod.getReturnMessageError("请选择帖子分类！");
        }
        if (content.isEmpty()) {
            return CommonMethod.getReturnMessageError("请输入帖子内容！");
        }
        Person currentPerson = getCurrentPerson();
        if (currentPerson == null) {
            return CommonMethod.getReturnMessageError("当前登录用户不存在！");
        }

        CommunityPost post = null;
        if (postId != null && postId > 0) {
            post = communityPostRepository.findById(postId).orElse(null);
            if (post == null) {
                return CommonMethod.getReturnMessageError("帖子不存在！");
            }
            if (!canManagePost(post)) {
                return CommonMethod.getReturnMessageError("只能修改自己发布的帖子！");
            }
        }
        if (post == null) {
            post = new CommunityPost();
            post.setAuthor(currentPerson);
            post.setCreatedTime(new Date());
        }
        post.setTitle(title);
        post.setCategory(category);
        post.setContent(content);
        post.setMediaType(mediaType);
        post.setMediaUrl(mediaUrl);
        post.setLinkUrl(linkUrl);
        post.setUpdatedTime(new Date());
        communityPostRepository.save(post);
        return CommonMethod.getReturnData(getPostDetailMap(post), "帖子保存成功。");
    }

    @Transactional
    public DataResponse postDelete(DataRequest dataRequest) {
        Integer postId = dataRequest.getInteger("communityPostId");
        if (postId == null || postId <= 0) {
            return CommonMethod.getReturnMessageError("帖子不存在！");
        }
        CommunityPost post = communityPostRepository.findById(postId).orElse(null);
        if (post == null) {
            return CommonMethod.getReturnMessageError("帖子不存在！");
        }
        if (!canManagePost(post)) {
            return CommonMethod.getReturnMessageError("只能删除自己发布的帖子！");
        }
        List<CommunityComment> commentList = communityCommentRepository.findByPostCommunityPostIdOrderByCreatedTimeAsc(postId);
        if (!commentList.isEmpty()) {
            communityCommentRepository.deleteAll(commentList);
        }
        communityPostRepository.delete(post);
        return CommonMethod.getReturnMessageOK("帖子删除成功。");
    }

    @Transactional
    public DataResponse commentSave(DataRequest dataRequest) {
        Integer postId = dataRequest.getInteger("communityPostId");
        String content = normalize(dataRequest.getString("content"));
        if (postId == null || postId <= 0) {
            return CommonMethod.getReturnMessageError("请选择要评论的帖子！");
        }
        if (content.isEmpty()) {
            return CommonMethod.getReturnMessageError("请输入评论内容！");
        }
        CommunityPost post = communityPostRepository.findById(postId).orElse(null);
        if (post == null) {
            return CommonMethod.getReturnMessageError("帖子不存在！");
        }
        Person currentPerson = getCurrentPerson();
        if (currentPerson == null) {
            return CommonMethod.getReturnMessageError("当前登录用户不存在！");
        }
        CommunityComment comment = new CommunityComment();
        comment.setPost(post);
        comment.setAuthor(currentPerson);
        comment.setContent(content);
        comment.setCreatedTime(new Date());
        communityCommentRepository.save(comment);
        return CommonMethod.getReturnData(getCommentMap(comment), "评论发布成功。");
    }

    @Transactional
    public DataResponse commentDelete(DataRequest dataRequest) {
        Integer commentId = dataRequest.getInteger("communityCommentId");
        if (commentId == null || commentId <= 0) {
            return CommonMethod.getReturnMessageError("评论不存在！");
        }
        CommunityComment comment = communityCommentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            return CommonMethod.getReturnMessageError("评论不存在！");
        }
        if (!"ROLE_ADMIN".equals(CommonMethod.getRoleName())) {
            return CommonMethod.getReturnMessageError("只有管理员可以删除评论。");
        }
        communityCommentRepository.delete(comment);
        return CommonMethod.getReturnMessageOK("评论删除成功。");
    }

    private boolean matchesFilter(CommunityPost post, String category, String keyword, boolean onlyMine) {
        if (onlyMine) {
            Integer personId = CommonMethod.getPersonId();
            if (personId == null || post.getAuthor() == null || !personId.equals(post.getAuthor().getPersonId())) {
                return false;
            }
        }
        if (!category.isEmpty() && !"全部".equals(category) && !category.equals(post.getCategory())) {
            return false;
        }
        if (keyword.isEmpty()) {
            return true;
        }
        String content = normalize(post.getContent());
        return normalize(post.getTitle()).contains(keyword)
                || content.contains(keyword)
                || normalize(post.getCategory()).contains(keyword);
    }

    private Map<String, Object> getPostSummaryMap(CommunityPost post) {
        Map<String, Object> map = new HashMap<>();
        map.put("communityPostId", post.getCommunityPostId());
        map.put("title", post.getTitle());
        map.put("category", post.getCategory());
        map.put("authorName", getPersonDisplayName(post.getAuthor()));
        map.put("authorRoleName", getRoleDisplayName(post.getAuthor()));
        map.put("createdTime", formatTime(post.getCreatedTime()));
        map.put("updatedTime", formatTime(post.getUpdatedTime()));
        map.put("commentCount", communityCommentRepository.findByPostCommunityPostIdOrderByCreatedTimeAsc(post.getCommunityPostId()).size());
        map.put("canEdit", canManagePost(post));
        map.put("summary", buildSummary(post.getContent()));
        map.put("mediaType", normalize(post.getMediaType()));
        map.put("mediaUrl", normalize(post.getMediaUrl()));
        map.put("linkUrl", normalize(post.getLinkUrl()));
        return map;
    }

    private Map<String, Object> getPostDetailMap(CommunityPost post) {
        Map<String, Object> map = getPostSummaryMap(post);
        map.put("content", post.getContent());
        map.put("authorNum", post.getAuthor() == null ? "" : post.getAuthor().getNum());
        return map;
    }

    private List<Map<String, Object>> getCommentMapList(Integer postId) {
        List<CommunityComment> commentList = communityCommentRepository.findByPostCommunityPostIdOrderByCreatedTimeAsc(postId);
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (CommunityComment comment : commentList) {
            dataList.add(getCommentMap(comment));
        }
        return dataList;
    }

    private Map<String, Object> getCommentMap(CommunityComment comment) {
        Map<String, Object> map = new HashMap<>();
        map.put("communityCommentId", comment.getCommunityCommentId());
        map.put("content", comment.getContent());
        map.put("authorName", getPersonDisplayName(comment.getAuthor()));
        map.put("authorRoleName", getRoleDisplayName(comment.getAuthor()));
        map.put("createdTime", formatTime(comment.getCreatedTime()));
        map.put("canDelete", "ROLE_ADMIN".equals(CommonMethod.getRoleName()));
        return map;
    }

    private void ensureDefaultCategories() {
        List<cn.edu.sdu.java.server.models.DictionaryInfo> rootList = dictionaryInfoRepository.findRootList();
        cn.edu.sdu.java.server.models.DictionaryInfo root = null;
        for (cn.edu.sdu.java.server.models.DictionaryInfo item : rootList) {
            if (CATEGORY_CODE.equals(item.getValue())) {
                root = item;
                break;
            }
        }
        if (root == null) {
            root = new cn.edu.sdu.java.server.models.DictionaryInfo();
            root.setPid(0);
            root.setValue(CATEGORY_CODE);
            root.setLabel("社区帖子分类");
            root = dictionaryInfoRepository.save(root);
        }
        List<cn.edu.sdu.java.server.models.DictionaryInfo> children = dictionaryInfoRepository.findByPid(root.getId());
        Set<String> existingValueSet = new LinkedHashSet<>();
        for (cn.edu.sdu.java.server.models.DictionaryInfo child : children) {
            existingValueSet.add(normalize(child.getValue()));
            existingValueSet.add(normalize(child.getLabel()));
        }
        for (String defaultCategory : DEFAULT_CATEGORY_LIST) {
            if (existingValueSet.contains(defaultCategory)) {
                continue;
            }
            cn.edu.sdu.java.server.models.DictionaryInfo child = new cn.edu.sdu.java.server.models.DictionaryInfo();
            child.setPid(root.getId());
            child.setValue(defaultCategory);
            child.setLabel(defaultCategory);
            dictionaryInfoRepository.save(child);
        }
    }

    private Person getCurrentPerson() {
        Integer personId = CommonMethod.getPersonId();
        if (personId == null) {
            return null;
        }
        return personRepository.findById(personId).orElse(null);
    }

    private boolean canManagePost(CommunityPost post) {
        if ("ROLE_ADMIN".equals(CommonMethod.getRoleName())) {
            return true;
        }
        Integer personId = CommonMethod.getPersonId();
        return personId != null
                && post.getAuthor() != null
                && personId.equals(post.getAuthor().getPersonId());
    }

    private String getPersonDisplayName(Person person) {
        if (person == null) {
            return "";
        }
        return normalize(person.getName()).isEmpty() ? person.getNum() : person.getNum() + "-" + person.getName();
    }

    private String getRoleDisplayName(Person person) {
        if (person == null) {
            return "";
        }
        return switch (normalize(person.getType())) {
            case "0" -> "管理员";
            case "1" -> "学生";
            case "2" -> "教师";
            default -> "";
        };
    }

    private String formatTime(Date date) {
        if (date == null) {
            return "";
        }
        synchronized (DATE_TIME_FORMAT) {
            return DATE_TIME_FORMAT.format(date);
        }
    }

    private String buildSummary(String content) {
        String cleanContent = normalize(content);
        if (cleanContent.length() <= 80) {
            return cleanContent;
        }
        return cleanContent.substring(0, 80) + "...";
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim();
    }
}
