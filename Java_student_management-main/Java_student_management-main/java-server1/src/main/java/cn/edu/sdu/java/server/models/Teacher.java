package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "teacher")
public class Teacher {

    @Id
    @Column(name = "person_id")
    private Integer personId;

    /**
     * 关键修改：
     * 1. cascade = CascadeType.ALL: 保存/更新/删除 Teacher 时，自动级联操作 Person。
     *    这对于 @MapsId 至关重要，因为它允许 JPA 先插入 Person 获取 ID，再填入 Teacher。
     * 2. fetch = FetchType.LAZY: 按需加载，性能更好（可选，但推荐）。
     */
    @MapsId
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    @Size(max = 20)
    @Column(length = 20)
    private String title;

    @Size(max = 10)
    @Column(length = 10)
    private String degree;
}
