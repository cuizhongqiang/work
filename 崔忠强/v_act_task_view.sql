-- 待办任务视图 --
-- ----------------------------
-- mysql
-- ----------------------------
DROP VIEW IF EXISTS	v_act_task_todo;
create view v_act_task_todo as
SELECT
	rt.ID_ AS ID,
	rt.PROC_INST_ID_ AS PID,
	hp.BUSINESS_KEY_ AS BUSINESS_KEY,
	hp.NAME_ AS BUSINESS_INFO,
	rt.PRIORITY_ AS PRIORITY,
	hp.START_USER_ID_ AS START_USER_ID,
	u.LAST_ AS start_user,
	rt.NAME_ AS NAME,
	rt.TASK_DEF_KEY_ AS ACTIVITY_ID,
	rt.ASSIGNEE_ AS ASSIGNEE,
	ri.USER_ID AS CANDIDATE,
	rp.KEY_ as PROCESS_DEF_KEY,
	rp.NAME_ as PROCESS_DEF_NAME,
	rp.VERSION_ as PROCESS_DEF_VERSION,
	rm.WO_CODE as WO_CODE,
	rm.WO_NAME as WO_NAME,
	rm.WO_URL as BUSINESS_URL,
	'waitfor' as TASK_TYPE,
	(
	CASE
		WHEN (isnull ( rt.ASSIGNEE_ ) AND isnull ( ri.USER_ID )) THEN '' 
		WHEN ( rt.ASSIGNEE_ IS NOT NULL ) THEN '已签收' 
		ELSE '待签收' 
	END 
	) AS status,
	rt.CREATE_TIME_ AS CREATE_TIME,
	rt.DUE_DATE_ AS DUE_DATE 
FROM
	act_ru_task rt 
	left join (
	
		SELECT
			i.TASK_ID_ AS TASK_ID_,
			i.USER_ID_ AS USER_ID 
		FROM
			 act_ru_identitylink i join act_ru_task t on i.TASK_ID_ = t.ID_
		WHERE
			(
				i.TASK_ID_ IS NOT NULL 
				AND ( i.USER_ID_ IS NOT NULL ) 
				AND isnull ( t.ASSIGNEE_ ) 
				AND ( i.TYPE_ = 'candidate' )
			)
		
	) ri ON rt.ID_ = ri.TASK_ID_ 
	left join act_hi_procinst hp ON rt.PROC_INST_ID_ = hp.PROC_INST_ID_ 
	left join act_re_procdef rp on rt.PROC_DEF_ID_ = rp.ID_ 
	left join act_id_user u ON hp.START_USER_ID_ = convert( u.ID_ using utf8 )
	left join (
		select rv.PROC_INST_ID_,mp.wo_code,mp.wo_name,mp.wo_url from act_ru_variable rv join act_ex_mapping mp 
		on rv.LONG_ = mp.ID 
		union
		select rv.PROC_INST_ID_,max(mp.wo_code),max(mp.wo_name),max(mp.wo_url) from act_ru_variable rv join act_ex_mapping mp 
		on rv.TEXT_ = mp.wo_code where rv.NAME_ != '@woType' group by rv.PROC_INST_ID_
	) rm on rm.PROC_INST_ID_ = rt.PROC_INST_ID_
union
SELECT
	nft.id AS ID,
	nft.pid AS PID,
	nft.business_key AS BUSINESS_KEY,
	nft.business_info AS BUSINESS_INFO,
	nft.priority AS PRIORITY,
	nft.start_user AS START_USER_ID,
	nft.start_user_name AS start_user,
	nft.task_name AS NAME,
	null AS ACTIVITY_ID,
	nft.assignee AS ASSIGNEE,
	null AS CANDIDATE,
	null as PROCESS_DEF_KEY,
	null as PROCESS_DEF_NAME,
	null as PROCESS_DEF_VERSION,
	nft.wo_code as WO_CODE,
	nft.wo_name as WO_NAME,
	nft.business_url as BUSINESS_URL,
	'noflow' as TASK_TYPE,
	(
	CASE  
		WHEN ( nft.status_ = 1 ) THEN '已签收' 
		ELSE '待签收' 
	END 
	) AS status,
	nft.create_time AS CREATE_TIME,
	nft.due_date AS DUE_DATE 
FROM
	act_ex_noflowtask nft
	where nft.status_ < 2;

-- ----------------------------
-- oracle
-- ----------------------------
CREATE OR REPLACE VIEW v_act_task_todo AS 
SELECT
	rt.ID_ AS ID,
	rt.PROC_INST_ID_ AS PID,
	hp.BUSINESS_KEY_ AS BUSINESS_KEY,
	hp.NAME_ AS BUSINESS_INFO,
	rt.PRIORITY_ AS PRIORITY,
	hp.START_USER_ID_ AS START_USER_ID,
	u.LAST_ AS start_user,
	rt.NAME_ AS NAME,
	rt.TASK_DEF_KEY_ AS ACTIVITY_ID,
	rt.ASSIGNEE_ AS ASSIGNEE,
	ri.USER_ID AS CANDIDATE,
	rp.KEY_ as PROCESS_DEF_KEY,
	rp.NAME_ as PROCESS_DEF_NAME,
	rp.VERSION_ as PROCESS_DEF_VERSION,
	rm.WO_CODE as WO_CODE,
	rm.WO_NAME as WO_NAME,
	rm.WO_URL as BUSINESS_URL,
	'waitfor' as TASK_TYPE,
	(
	CASE	
		WHEN ( rt.ASSIGNEE_ IS NULL AND ri.USER_ID IS NULL ) THEN '' 
		WHEN ( rt.ASSIGNEE_ IS NOT NULL ) THEN '已签收' 
		ELSE '待签收' 
	END 
	) AS STATUS,
	rt.CREATE_TIME_ AS CREATE_TIME,
	rt.DUE_DATE_ AS DUE_DATE 
FROM
	act_ru_task rt
	LEFT JOIN (
		SELECT
			to_char(i.TASK_ID_) AS TASK_ID_,
			to_char(i.USER_ID_) AS USER_ID 
		FROM
			 act_ru_identitylink i JOIN act_ru_task t on i.TASK_ID_ = t.ID_
		WHERE
			(
				( i.TASK_ID_ IS NOT NULL ) 
				AND ( i.USER_ID_ IS NOT NULL ) 
				AND t.ASSIGNEE_ IS NULL 
				AND ( i.TYPE_ = 'candidate' ) 
			)
	) ri ON rt.ID_ = ri.TASK_ID_ 
	LEFT JOIN act_hi_procinst hp ON rt.PROC_INST_ID_ = hp.PROC_INST_ID_
	left join act_re_procdef rp on rt.PROC_DEF_ID_ = rp.ID_ 
	LEFT JOIN act_id_user u ON hp.START_USER_ID_ = to_char ( u.ID_ )
	left join (
		select rv.PROC_INST_ID_,mp.wo_code,mp.wo_name,mp.wo_url from act_ru_variable rv join act_ex_mapping mp 
		on rv.LONG_ = mp.ID 
		union
		select rv.PROC_INST_ID_,max(mp.wo_code),max(mp.wo_name),max(mp.wo_url) from act_ru_variable rv join act_ex_mapping mp 
		on rv.TEXT_ = mp.wo_code where rv.NAME_ != '@woType' group by rv.PROC_INST_ID_
	) rm on rm.PROC_INST_ID_ = rt.PROC_INST_ID_
union
SELECT
	Translate(nft.id USING NCHAR_CS) AS ID,
	nft.pid AS PID,
	nft.business_key AS BUSINESS_KEY,
	nft.business_info AS BUSINESS_INFO,
	nft.priority AS PRIORITY,
	nft.start_user AS START_USER_ID,
	nft.start_user_name AS start_user,
	nft.task_name AS NAME,
	null AS ACTIVITY_ID,
	nft.assignee AS ASSIGNEE,
	null AS CANDIDATE,
	null as PROCESS_DEF_KEY,
	null as PROCESS_DEF_NAME,
	null as PROCESS_DEF_VERSION,
	nft.wo_code as WO_CODE,
	nft.wo_name as WO_NAME,
	nft.business_url as BUSINESS_URL,
	'noflow' as TASK_TYPE,
	(
	CASE  
		WHEN ( nft.status_ = 1 ) THEN '已签收' 
		ELSE '待签收' 
	END 
	) AS status,
	nft.create_time AS CREATE_TIME,
	nft.due_date AS DUE_DATE 
FROM
	act_ex_noflowtask nft
	where nft.status_ < 2;

-- 已办任务视图 --
-- ----------------------------
-- mysql
-- ----------------------------
DROP VIEW IF EXISTS	v_act_task_havedone;
create view v_act_task_havedone as
SELECT 
	t_hi.ID_ as id,
	t_hi.PROC_INST_ID_ as pid,
	t_hi.ASSIGNEE_ as assignee,
	p_hi.PROC_DEF_ID_ as proc_def_id,
	p_hi.BUSINESS_KEY_ as business_key,
	p_hi.NAME_ as business_info,
	p_hi.START_USER_ID_ as start_user,
	u.LAST_ AS start_user_name,
	t_hi.TASK_DEF_KEY_ as activity_id,
	t_hi.PRIORITY_ as priority,
	t_hi.START_TIME_ as start_time,
	t_hi.CLAIM_TIME_ as claim_time,
	t_hi.END_TIME_ as end_time,
	'transated' as task_type
FROM 
	act_hi_taskinst t_hi 
INNER JOIN (
	SELECT PROC_INST_ID_,ASSIGNEE_,MAX(ID_) ID_ FROM act_hi_taskinst t WHERE 
		END_TIME_ is not null 
		and 
		DELETE_REASON_ = 'completed' 
		and 
		ASSIGNEE_ is not NULL 
		-- and ID_ not in (select c.TASK_ID_ from act_hi_comment c where t.ID_ = c.TASK_ID_ and c.TYPE_ = 'option' and c.MESSAGE_ = '自动审批') 
		GROUP BY PROC_INST_ID_,ASSIGNEE_) t_hi_max on t_hi.ID_ = t_hi_max.ID_

LEFT JOIN act_hi_procinst p_hi on t_hi.PROC_INST_ID_ = p_hi.PROC_INST_ID_
left join act_id_user u ON p_hi.START_USER_ID_ = convert( u.ID_ using utf8 )
union
SELECT
	nft.id as id,
	nft.pid as pid,
	nft.assignee as assignee,
	null as proc_def_id,
	nft.business_key as business_key,
	nft.business_info as business_info,
	nft.start_user as start_user,
	nft.start_user_name AS start_user_name,
	null as activity_id,
	nft.priority as priority,
	nft.create_time as start_time,
	nft.claim_time as claim_time,
	nft.lastmodified_time as end_time,
	'noflow' as task_type
FROM
	act_ex_noflowtask nft
	where nft.status_ = 2;

-- ----------------------------
-- oracle
-- ----------------------------
CREATE OR REPLACE VIEW v_act_task_havedone as
SELECT
	t_hi.ID_ as id,
	t_hi.PROC_INST_ID_ as pid,
	t_hi.ASSIGNEE_ as assignee,
	p_hi.PROC_DEF_ID_ as proc_def_id,
	p_hi.BUSINESS_KEY_ as business_key,
	p_hi.NAME_ as business_info,
	p_hi.START_USER_ID_ as start_user,
	u.LAST_ AS start_user_name,
	t_hi.TASK_DEF_KEY_ as activity_id,
	t_hi.PRIORITY_ as priority,
	t_hi.START_TIME_ as start_time,
	t_hi.CLAIM_TIME_ as claim_time,
	t_hi.END_TIME_ as end_time,
	'transated' as task_type
FROM 
	act_hi_taskinst t_hi 
INNER JOIN (SELECT PROC_INST_ID_,ASSIGNEE_,MAX(ID_) ID_ FROM act_hi_taskinst WHERE END_TIME_ is not null and to_char(DELETE_REASON_) = 'completed' 
and ASSIGNEE_ is not NULL GROUP BY PROC_INST_ID_,ASSIGNEE_) t_hi_max on t_hi.ID_ = t_hi_max.ID_
LEFT JOIN act_hi_procinst p_hi on t_hi.PROC_INST_ID_ = p_hi.PROC_INST_ID_
LEFT JOIN act_id_user u ON p_hi.START_USER_ID_ = to_char ( u.ID_ )
union
SELECT
	Translate(nft.id USING NCHAR_CS) as id,
	nft.pid as pid,
	nft.assignee as assignee,
	null as proc_def_id,
	nft.business_key as business_key,
	nft.business_info as business_info,
	nft.start_user as start_user,
	nft.start_user_name AS start_user_name,
	null as activity_id,
	nft.priority as priority,
	nft.create_time as start_time,
	nft.claim_time as claim_time,
	nft.lastmodified_time as end_time,
	'noflow' as task_type
FROM
	act_ex_noflowtask nft
	where nft.status_ = 2;