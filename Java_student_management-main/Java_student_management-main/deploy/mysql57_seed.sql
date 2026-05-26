SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Import this file after the backend jar has been started once so that
-- Hibernate can create the tables automatically.
-- Default login password for all sample users: 123456

INSERT INTO user_type(id, name)
VALUES
  (1, 'ROLE_ADMIN'),
  (2, 'ROLE_STUDENT'),
  (3, 'ROLE_TEACHER')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO person(person_id, type, num, name, dept, gender, birthday, phone, address, email, introduce)
VALUES
  (1, '0', 'admin', '系统管理员', '系统管理', '1', '1990-01-01', '13800000001', '济南', 'admin@example.com', '默认管理员账号'),
  (2, '1', '20230001', '张三', '软件学院', '1', '2003-09-01', '13800000002', '济南', 'student@example.com', '默认学生账号'),
  (3, '2', 'T2023001', '李老师', '软件学院', '1', '1988-03-12', '13800000003', '济南', 'teacher@example.com', '默认教师账号')
ON DUPLICATE KEY UPDATE
  type = VALUES(type),
  num = VALUES(num),
  name = VALUES(name),
  dept = VALUES(dept),
  gender = VALUES(gender),
  birthday = VALUES(birthday),
  phone = VALUES(phone),
  address = VALUES(address),
  email = VALUES(email),
  introduce = VALUES(introduce);

INSERT INTO user(person_id, user_type_id, user_name, password, create_time, creator_id, login_count)
VALUES
  (1, 1, 'admin', '$2a$10$ZTlDFEE2RmX1mn.QzZLaE.BFbu4oOfWvINlc9SGsJY8J2jxuJy5I2', '2026-04-21 20:00:00', 1, 0),
  (2, 2, '20230001', '$2a$10$ZTlDFEE2RmX1mn.QzZLaE.BFbu4oOfWvINlc9SGsJY8J2jxuJy5I2', '2026-04-21 20:00:00', 1, 0),
  (3, 3, 'T2023001', '$2a$10$ZTlDFEE2RmX1mn.QzZLaE.BFbu4oOfWvINlc9SGsJY8J2jxuJy5I2', '2026-04-21 20:00:00', 1, 0)
ON DUPLICATE KEY UPDATE
  user_type_id = VALUES(user_type_id),
  user_name = VALUES(user_name),
  password = VALUES(password),
  create_time = VALUES(create_time),
  creator_id = VALUES(creator_id),
  login_count = VALUES(login_count);

INSERT INTO student(person_id, major, class_name)
VALUES
  (2, '软件工程', '软件2301')
ON DUPLICATE KEY UPDATE
  major = VALUES(major),
  class_name = VALUES(class_name);

INSERT INTO teacher(person_id, degree, title)
VALUES
  (3, '硕士', '讲师')
ON DUPLICATE KEY UPDATE
  degree = VALUES(degree),
  title = VALUES(title);

INSERT INTO menu(id, name, title, pid, user_type_ids)
VALUES
  (3, NULL, '人员管理', NULL, '1'),
  (4, NULL, '教学分析', NULL, '1,3'),
  (31, 'student-panel', '学生管理', 3, '1'),
  (32, 'student-leave-panel', '学生请假', 4, '2,3'),
  (43, 'student-statistics-panel', '学生统计', 4, '1,3')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  title = VALUES(title),
  pid = VALUES(pid),
  user_type_ids = VALUES(user_type_ids);

INSERT INTO dictionary(id, value, label, pid)
VALUES
  (10, 'SHZTM', '审核状态', NULL),
  (11, '0', '待审核', 10),
  (12, '1', '已通过', 10),
  (13, '2', '已驳回', 10),
  (20, 'COMMUNITY_CATEGORY', '社区帖子分类', NULL),
  (21, '综合交流', '综合交流', 20),
  (22, '课程讨论', '课程讨论', 20),
  (23, '校园互助', '校园互助', 20),
  (24, '学习分享', '学习分享', 20)
ON DUPLICATE KEY UPDATE
  value = VALUES(value),
  label = VALUES(label),
  pid = VALUES(pid);

INSERT INTO student_leave(student_leave_id, student_id, teacher_id, leave_date, reason, state, apply_time, teacher_comment, admin_comment)
VALUES
  (1, 2, 3, '2026-04-21', '云服务器部署演示请假单', 0, NOW(), '', '')
ON DUPLICATE KEY UPDATE
  student_id = VALUES(student_id),
  teacher_id = VALUES(teacher_id),
  leave_date = VALUES(leave_date),
  reason = VALUES(reason),
  state = VALUES(state),
  apply_time = VALUES(apply_time),
  teacher_comment = VALUES(teacher_comment),
  admin_comment = VALUES(admin_comment);

INSERT INTO student_statistics(statistics_id, person_id, course_count, avg_score, credit_total, gpa, leave_count, no, year, active_count)
VALUES
  (1, 2, 5, 88.0, 16, 3.8, 1, 1, '2026', 6)
ON DUPLICATE KEY UPDATE
  person_id = VALUES(person_id),
  course_count = VALUES(course_count),
  avg_score = VALUES(avg_score),
  credit_total = VALUES(credit_total),
  gpa = VALUES(gpa),
  leave_count = VALUES(leave_count),
  no = VALUES(no),
  year = VALUES(year),
  active_count = VALUES(active_count);

INSERT INTO community_post(community_post_id, title, category, content, media_type, media_url, link_url, author_id, created_time, updated_time)
VALUES
  (1, '欢迎来到校园社区', '综合交流', '这是部署后的第一条示例帖子。你可以用学生账号或老师账号继续发帖、评论，验证社区功能是否正常。', '', '', '', 2, '2026-04-21 20:10:00', '2026-04-21 20:10:00')
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  category = VALUES(category),
  content = VALUES(content),
  media_type = VALUES(media_type),
  media_url = VALUES(media_url),
  link_url = VALUES(link_url),
  author_id = VALUES(author_id),
  created_time = VALUES(created_time),
  updated_time = VALUES(updated_time);

INSERT INTO community_comment(community_comment_id, post_id, author_id, content, created_time)
VALUES
  (1, 1, 3, '老师账号的示例评论，方便你验证帖子详情和评论列表。', '2026-04-21 20:20:00')
ON DUPLICATE KEY UPDATE
  post_id = VALUES(post_id),
  author_id = VALUES(author_id),
  content = VALUES(content),
  created_time = VALUES(created_time);

SET FOREIGN_KEY_CHECKS = 1;
