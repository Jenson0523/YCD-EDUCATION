/**
 * 模块: 教务 edu
 * 功能: 教务模块 API
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

import { http } from './http';

export const fetchSubjects = () => http.get('/edu/subjects');
export const createSubject = (data) => http.post('/edu/subjects', data);
export const updateSubject = (id, data) => http.put(`/edu/subjects/${id}`, data);
export const deleteSubject = (id) => http.delete(`/edu/subjects/${id}`);

export const fetchGrades = () => http.get('/edu/grades');
export const createGrade = (data) => http.post('/edu/grades', data);

export const fetchClasses = (params) => http.get('/edu/classes', { params });
export const createClass = (data) => http.post('/edu/classes', data);

export const fetchSchedules = (params) => http.get('/edu/schedules', { params });
export const createSchedule = (data) => http.post('/edu/schedules', data);
export const deleteSchedule = (id) => http.delete(`/edu/schedules/${id}`);

export const fetchScores = (params) => http.get('/edu/scores', { params });
export const createScore = (data) => http.post('/edu/scores', data);
export const updateScore = (id, data) => http.put(`/edu/scores/${id}`, data);

export const fetchHomeworks = (params) => http.get('/edu/homeworks', { params });
export const createHomework = (data) => http.post('/edu/homeworks', data);

export const fetchTeachingProgress = (params) => http.get('/edu/teaching-progress', { params });
export const createTeachingProgress = (data) => http.post('/edu/teaching-progress', data);

export const fetchStudentHonors = (params) => http.get('/edu/student-honors', { params });
export const approveStudentHonor = (id) => http.put(`/edu/student-honors/${id}/approve`);
export const rejectStudentHonor = (id) => http.put(`/edu/student-honors/${id}/reject`);
