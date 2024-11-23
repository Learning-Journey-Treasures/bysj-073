package com.ssm.guard.controller;

import com.alibaba.fastjson.JSONObject;
import com.ssm.guard.po.*;
import com.ssm.guard.service.ApplyService;
import com.ssm.guard.service.LabinfoService;
import com.ssm.guard.service.RepairService;
import com.ssm.guard.service.StudentService;
import com.ssm.guard.util.JwtUtils;
import com.ssm.guard.util.Result;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: 小程序控制层
 * @author: mty
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(value = "/api")
public class ApiController {

    // 依赖注入
    @Autowired
    private StudentService studentService;
    @Autowired
    private LabinfoService labinfoService;
    @Autowired
    private ApplyService applyService;
    @Autowired
    private RepairService repairService;


    /**
     * 登录
     */
    @RequestMapping(value = "/login")
    public Result findActicle(@RequestBody Student student) {
        try {
            if (StringUtils.isEmpty(student.getSno())) {
                return Result.error("用户不能为空");
            }
            if (StringUtils.isEmpty(student.getPassword())) {
                return Result.error("密码不能为空");
            }
            Map mp = new HashMap();
            mp.put("sno", student.getSno());
            List<Student> list = studentService.queryFilter(mp);
            if (list.size() > 1) {
                return Result.error("存在多个用户");
            } else if (list.size() == 0) {
                return Result.error("查无此用户");
            } else {
                // 查到用户
                Student loginStudent = list.get(0);
                if (!student.getPassword().equals(loginStudent.getPassword())) {
                    return Result.error("密码错误");
                } else {
                    loginStudent.setPassword(null);
                    JSONObject obj = new JSONObject();
                    obj.put("loginUser", loginStudent);
                    obj.put("token", JwtUtils.createToken(loginStudent.getId() + "", loginStudent.getSno(), loginStudent.getName(), "student"));
                    return Result.ok(obj,"登录成功");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("登录失败");
        }
    }


    /**
     * 注册
     */
    @RequestMapping(value = "/register")
    public Result register(@RequestBody Student student) {
        try {
            Map mp = new HashMap();
            mp.put("sno", student.getSno());
            List<Student> list = studentService.queryFilter(mp);
            if (list.size() >= 1) {
                return Result.error("该学号已经注册，请更换！");
            } else {
                // 查到用户
                SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                student.setCreateTime(sf.format(new Date()));
                studentService.addStudent(student);
                return Result.ok("注册成功");
            }

        } catch (Exception e) {
            return Result.error("注册失败");
        }
    }


    /**
     * 更新信息
     */
    @RequestMapping(value = "/updateInfo")
    public Result updateInfo(@RequestBody Student student) {
        try {
            List<Student> list = studentService.getAll();
            for(int i=0;i<list.size();i++){
                if(list.get(i).getSno().equals(student.getSno()) && !list.get(i).getId().equals(student.getId())){
                    return Result.error("该用户已经注册，请更换学号！");
                }
            }
            // 更新
            studentService.updateStudent(student);
            JSONObject obj = new JSONObject();
            Student studentById = studentService.findStudentById(student.getId());
            obj.put("loginUser",studentById);
            obj.put("token", JwtUtils.createToken(student.getId() + "", student.getPhone(), student.getName(), "01"));
            return Result.ok(obj,"更新成功");
        } catch (Exception e) {
            return Result.error("更新失败");
        }
    }

    // 验证token
    @RequestMapping(value = "/validate", method = RequestMethod.POST)
    public Result validate(@RequestBody UserToken userToken) {
        System.out.println(">>>" + userToken);
        try {
            String studentId = JwtUtils.getAudience(userToken.getToken());
            System.out.println(">>>" + studentId);
            JwtUtils.verifyToken(userToken.getToken(), studentId);
            return Result.ok("验证成功");
        } catch (Exception e) {
            e.printStackTrace();
            Result result = Result.error("验证失败");
            result.setCode(510);
            return result;
        }

    }


    /**
     * 首页获取教室信息
     */
    @RequestMapping(value = "/queryAllLabInfo")
    public Result queryAllCourse() {
        try {
            Map mp = new HashMap();
            List<Labinfo> list = labinfoService.queryFilter(mp);
            Result result = new Result();
            result.setData(list);
            result.setCode(200);
            result.setMessage("查询成功");
            return result;
        } catch (Exception e) {
            return Result.error("查询失败");
        }
    }


    /**
     * 根据ID获取课程信息
     */
    @RequestMapping(value = "/queryLabById")
    public Result queryCourseById(String id) {
        try {
            Map mp = new HashMap();
            mp.put("id", id);
            List<Labinfo> list = labinfoService.queryFilter(mp);
            Result result = new Result();
            result.setData(list);
            result.setCode(200);
            result.setMessage("查询成功");
            return result;
        } catch (Exception e) {
            return Result.error("查询失败");
        }
    }



    /**
     * 预订
     */
    @RequestMapping(value = "/selectLab")
    public Result selectCourse(@RequestBody Apply apply) {
        try {
            Map mp = new HashMap();
            mp.put("lid", apply.getLid());
            mp.put("sid", apply.getSid());
            List<Apply> list = applyService.queryFilter(mp);
            if(list.size()>0){
                return Result.error("该教室已选！");
            }
            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            apply.setCreateTime(sf.format(new Date()));
            apply.setStatus("01");
            applyService.addApply(apply);
            //更新实验室状态
            Labinfo labinfo = new Labinfo();
            labinfo.setId(apply.getLid());
            labinfo.setStatus("02");
            labinfoService.updateLabinfo(labinfo);
            Result result = new Result();
            result.setData(list);
            result.setCode(200);
            result.setMessage("申请成功");
            return result;
        } catch (Exception e) {
            return Result.error("申请失败");
        }
    }



    /**
     * 获取个人预订信息
     */
    @RequestMapping(value = "/myApply")
    public Result mycourse(String id, String type) {
        try {
            Result result = new Result();
            Map mp = new HashMap();
            mp.put("sid", id);
            List<Apply> list = applyService.queryFilter(mp);
            if(list.size()>0){
                for(int i=0;i<list.size();i++){
                    Labinfo labinfo = labinfoService.findLabinfoById(list.get(i).getLid());
                    list.get(i).setLabinfo(labinfo);
                }
            }
            result.setData(list);
            result.setCode(200);
            result.setMessage("查询成功");
            return result;
        } catch (Exception e) {
            return Result.error("查询失败");
        }
    }


    /**
     * 取消预订
     */
    @RequestMapping(value = "/tuiding")
    public Result tuike(String lid, String id) {
        try {
            applyService.deleteApply(id);
            //更新实验室状态
            Labinfo labinfo = new Labinfo();
            labinfo.setId(lid);
            labinfo.setStatus("01");
            labinfoService.updateLabinfo(labinfo);
            Result result = new Result();
            result.setCode(200);
            result.setMessage("取消预订成功");
            return result;
        } catch (Exception e) {
            return Result.error("取消预订失败");
        }
    }

    /**
     * 查看预订某个教室的学生
     */
    @RequestMapping(value = "/queryStudent")
    public Result queryStudent(String lid) {
        try {
            Map mp = new HashMap<>();
            mp.put("lid",lid);
            List<Apply> applies = applyService.queryFilter(mp);
            Result result = new Result();
            result.setData(applies);
            result.setCode(200);
            result.setMessage("查询成功");
            return result;
        } catch (Exception e) {
            return Result.error("查询失败");
        }
    }



    /**
     * 查询报修信息
     */
    @RequestMapping(value = "/getProblemList")
    public Result getProblemList(String sid) {
        try {
            Map mp = new HashMap();
            mp.put("sid", sid);
            List<Repair> messageList = repairService.queryFilter(mp);
            Result result = new Result();
            result.setCode(200);
            result.setData(messageList);
            result.setMessage("查询成功");
            return result;
        } catch (Exception e) {
            return Result.error("查询失败");
        }
    }



    /**
     * 增加报修
     */
    @RequestMapping(value = "/addProblemByInfo")
    public Result addProblemByInfo(String id,String content) {
        try {
            Repair repair = new Repair();
            repair.setContent(content);
            repair.setSid(id);
            Date d = new Date();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String time = simpleDateFormat.format(d);
            repair.setCreateTime(time);
            repairService.addRepair(repair);
            Result result = new Result();
            result.setCode(200);
            result.setMessage("提交成功");
            return result;
        } catch (Exception e) {
            return Result.error("提交失败");
        }
    }



}
