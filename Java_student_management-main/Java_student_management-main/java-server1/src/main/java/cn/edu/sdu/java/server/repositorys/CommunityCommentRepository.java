package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Integer> {
    List<CommunityComment> findByPostCommunityPostIdOrderByCreatedTimeAsc(Integer communityPostId);
}
