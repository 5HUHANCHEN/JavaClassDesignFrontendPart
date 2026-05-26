package cn.edu.sdu.java.server.models;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "community_post")
public class CommunityPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer communityPostId;

    @Size(max = 100)
    private String title;

    @Size(max = 30)
    private String category;

    @Size(max = 4000)
    private String content;

    @Size(max = 20)
    private String mediaType;

    @Size(max = 500)
    private String mediaUrl;

    @Size(max = 500)
    private String linkUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Person author;

    private Date createdTime;

    private Date updatedTime;
}
