INSERT INTO menu(id,name,title,pid,user_type_ids)
VALUES (32,'student-leave-panel','学生请假',3,'2,3')
ON DUPLICATE KEY UPDATE name=VALUES(name), title=VALUES(title), pid=VALUES(pid), user_type_ids=VALUES(user_type_ids);

INSERT INTO menu(id,name,title,pid,user_type_ids)
VALUES (43,'student-statistics-panel','学生统计',4,'1,3')
ON DUPLICATE KEY UPDATE name=VALUES(name), title=VALUES(title), pid=VALUES(pid), user_type_ids=VALUES(user_type_ids);

INSERT INTO dictionary(id,value,label,pid)
VALUES (10,'SHZTM','审核状态',NULL)
ON DUPLICATE KEY UPDATE value=VALUES(value), label=VALUES(label), pid=VALUES(pid);

INSERT INTO dictionary(id,value,label,pid)
VALUES (11,'0','待审核',10),
       (12,'1','已通过',10),
       (13,'2','已驳回',10)
ON DUPLICATE KEY UPDATE value=VALUES(value), label=VALUES(label), pid=VALUES(pid);

INSERT INTO person(person_id,type,num,name,dept,gender,birthday,phone,address,email,introduce)
VALUES (5,'2','022200','测试教师','软件学院','1','1985-09-01','13800002200','山东大学软件学院','022200@sdu.edu.cn','用于 JavaFX 教师端功能测试的示例教师账号。')
ON DUPLICATE KEY UPDATE type=VALUES(type), num=VALUES(num), name=VALUES(name), dept=VALUES(dept), gender=VALUES(gender), birthday=VALUES(birthday), phone=VALUES(phone), address=VALUES(address), email=VALUES(email), introduce=VALUES(introduce);

INSERT INTO teacher(person_id,degree,title)
VALUES (5,'硕士','副教授')
ON DUPLICATE KEY UPDATE degree=VALUES(degree), title=VALUES(title);

INSERT INTO user(person_id,user_type_id,user_name,password,create_time,creator_id,login_count)
VALUES (5,3,'022200','$2a$10$FV5lm..jdQWmV7hFguxKDeTrGyiWg1u6HYD2QiQc0tRROrNtSQVOy','2026-04-09 20:00:00',1,0)
ON DUPLICATE KEY UPDATE user_type_id=VALUES(user_type_id), user_name=VALUES(user_name), password=VALUES(password), create_time=VALUES(create_time), creator_id=VALUES(creator_id), login_count=VALUES(login_count);

INSERT INTO student_leave(student_leave_id,student_id,teacher_id,leave_date,reason,state,apply_time,teacher_comment,admin_comment)
VALUES (1,2,5,'2026-04-15','课程项目演示请假',0,NOW(),'','')
ON DUPLICATE KEY UPDATE student_id=VALUES(student_id), teacher_id=VALUES(teacher_id), leave_date=VALUES(leave_date), reason=VALUES(reason), state=VALUES(state), teacher_comment=VALUES(teacher_comment), admin_comment=VALUES(admin_comment);

INSERT INTO student_statistics(statistics_id,person_id,course_count,avg_score,credit_total,gpa,leave_count,no,year,active_count)
VALUES (1,2,5,88.0,16,3.8,1,1,'2026',6),
       (2,3,4,82.0,12,3.4,0,2,'2026',4)
ON DUPLICATE KEY UPDATE course_count=VALUES(course_count), avg_score=VALUES(avg_score), credit_total=VALUES(credit_total), gpa=VALUES(gpa), leave_count=VALUES(leave_count), no=VALUES(no), year=VALUES(year), active_count=VALUES(active_count);