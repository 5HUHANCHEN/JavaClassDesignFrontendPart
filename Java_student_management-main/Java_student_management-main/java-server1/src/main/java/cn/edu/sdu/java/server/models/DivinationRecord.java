package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "divination_record")
public class DivinationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer recordId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Person user;

    @Size(max = 50)
    private String userName;

    @Size(max = 30)
    private String type;

    @Size(max = 200)
    private String question;

    @Column(length = 2000)
    private String background;

    @Size(max = 50)
    private String method;

    @Column(length = 4000)
    private String inputJson;

    @Column(length = 4000)
    private String resultJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String reportText;

    @Size(max = 20)
    private String createTime;
}
