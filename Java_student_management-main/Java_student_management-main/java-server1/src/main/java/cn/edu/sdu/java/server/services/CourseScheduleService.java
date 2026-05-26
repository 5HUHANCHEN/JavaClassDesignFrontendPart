package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.CourseSchedule;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CourseScheduleRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CourseScheduleService {
    private final CourseScheduleRepository courseScheduleRepository;

    public CourseScheduleService(CourseScheduleRepository courseScheduleRepository) {
        this.courseScheduleRepository = courseScheduleRepository;
    }

    public DataResponse getCourseScheduleList(DataRequest dataRequest) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        List<CourseSchedule> scheduleList = courseScheduleRepository.findAllByOrderByDayOfWeekAscStartTimeAscStartWeekAsc();
        for (CourseSchedule schedule : scheduleList) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", schedule.getId());
            item.put("courseId", schedule.getCourseId());
            item.put("name", schedule.getName());
            item.put("dayOfWeek", schedule.getDayOfWeek());
            item.put("startTime", schedule.getStartTime());
            item.put("startWeek", schedule.getStartWeek());
            item.put("stopWeek", schedule.getStopWeek());
            item.put("start_week", schedule.getStartWeek());
            item.put("stop_week", schedule.getStopWeek());
            dataList.add(item);
        }
        return CommonMethod.getReturnData(dataList);
    }

    public DataResponse courseScheduleSave(DataRequest dataRequest) {
        Integer id = dataRequest.getInteger("id");
        String name = dataRequest.getString("name");
        Integer dayOfWeek = dataRequest.getInteger("dayOfWeek");
        String startTime = dataRequest.getString("startTime");
        Integer startWeek = dataRequest.getInteger("startWeek");
        Integer stopWeek = dataRequest.getInteger("stopWeek");

        if (name == null || name.trim().isEmpty()) {
            return CommonMethod.getReturnMessageError("课程名称不能为空！");
        }
        if (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7) {
            return CommonMethod.getReturnMessageError("星期几必须在 1 到 7 之间！");
        }
        if (startTime == null || startTime.trim().isEmpty()) {
            return CommonMethod.getReturnMessageError("上课时间不能为空！");
        }
        if (startWeek == null || startWeek < 1) {
            return CommonMethod.getReturnMessageError("开始周必须大于 0！");
        }
        if (stopWeek == null || stopWeek < startWeek) {
            return CommonMethod.getReturnMessageError("结束周不能小于开始周！");
        }

        String conflictMessage = checkCourseConflict(dayOfWeek, startTime, startWeek, stopWeek, id);
        if (conflictMessage != null) {
            return CommonMethod.getReturnMessageError("课表冲突：" + conflictMessage);
        }

        CourseSchedule courseSchedule = id == null
                ? new CourseSchedule()
                : courseScheduleRepository.findById(id).orElseGet(CourseSchedule::new);

        courseSchedule.setCourseId(dataRequest.getInteger("courseId"));
        courseSchedule.setName(name.trim());
        courseSchedule.setDayOfWeek(dayOfWeek);
        courseSchedule.setStartTime(startTime.trim());
        courseSchedule.setStartWeek(startWeek);
        courseSchedule.setStopWeek(stopWeek);
        courseScheduleRepository.save(courseSchedule);
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse courseScheduleDelete(DataRequest dataRequest) {
        Integer id = dataRequest.getInteger("id");
        if (id == null) {
            return CommonMethod.getReturnMessageError("课表记录编号不能为空！");
        }
        if (!courseScheduleRepository.existsById(id)) {
            return CommonMethod.getReturnMessageError("课表记录不存在！");
        }
        courseScheduleRepository.deleteById(id);
        return CommonMethod.getReturnMessageOK();
    }

    private String checkCourseConflict(Integer dayOfWeek,
                                       String startTime,
                                       Integer startWeek,
                                       Integer stopWeek,
                                       Integer excludeId) {
        List<CourseSchedule> existingList = courseScheduleRepository.findAll();
        for (CourseSchedule existing : existingList) {
            if (excludeId != null && excludeId.equals(existing.getId())) {
                continue;
            }
            if (!dayOfWeek.equals(existing.getDayOfWeek())) {
                continue;
            }
            if (!normalizeTime(startTime).equals(normalizeTime(existing.getStartTime()))) {
                continue;
            }
            if (isWeekRangeOverlap(startWeek, stopWeek, existing.getStartWeek(), existing.getStopWeek())) {
                return existing.getName() + "（第 " + existing.getStartWeek() + "-" + existing.getStopWeek() + " 周，" + existing.getStartTime() + "）";
            }
        }
        return null;
    }

    private boolean isWeekRangeOverlap(int start1, int stop1, int start2, int stop2) {
        return start1 <= stop2 && start2 <= stop1;
    }

    private String normalizeTime(String time) {
        return time == null ? "" : time.replaceFirst("^0+(?!$)", "");
    }
}
