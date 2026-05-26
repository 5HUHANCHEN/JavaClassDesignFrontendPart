package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Integer> {
    List<CommunityPost> findAllByOrderByUpdatedTimeDescCreatedTimeDesc();
}
