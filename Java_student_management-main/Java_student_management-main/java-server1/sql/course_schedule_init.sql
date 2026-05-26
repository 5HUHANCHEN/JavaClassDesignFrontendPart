CREATE TABLE IF NOT EXISTS course_schedule (
    id INT PRIMARY KEY AUTO_INCREMENT,
    course_id INT NULL,
    name VARCHAR(100) NOT NULL,
    day_of_week INT NOT NULL,
    start_time VARCHAR(20) NOT NULL,
    start_week INT NOT NULL,
    stop_week INT NOT NULL
);
