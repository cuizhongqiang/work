-- mysql
DROP VIEW IF EXISTS	v_act_procinst;
create view v_act_procinst as
select 

hp.*,
u.LAST_ as start_user_name,
pti.transactor as transactor,
pti.transactor_name as transactor_name,
pti.SUSPENSION_STATE_ as SUSPENSION_STATE_,
hm.WO_CODE as WO_CODE,
hm.WO_NAME as WO_NAME,
hm.WO_URL as WO_URL,
ep.status as status_

from act_hi_procinst hp 

left join (
	select 
		GROUP_CONCAT(DISTINCT transactor) as transactor,
		GROUP_CONCAT(DISTINCT transactor_name) as transactor_name,
		PROC_INST_ID_,
		max(SUSPENSION_STATE_) as SUSPENSION_STATE_
	from
	(
		select 
		
		if(GROUP_CONCAT(rt.ASSIGNEE_) is null, GROUP_CONCAT(ri.USER_ID_), GROUP_CONCAT(rt.ASSIGNEE_)) as transactor,
		GROUP_CONCAT(u.Last_) AS transactor_name,
		MAX(rt.PROC_INST_ID_) as PROC_INST_ID_,
		max(rt.SUSPENSION_STATE_) as SUSPENSION_STATE_
		
		from act_ru_task rt 
		left join act_ru_identitylink ri on ri.TASK_ID_ = rt.ID_ and ri.TYPE_ = 'candidate'
		left join act_id_user u ON
		if(rt.ASSIGNEE_ is null, ri.USER_ID_, rt.ASSIGNEE_) = u.ID_
		group by rt.ID_
	) rti group by rti.PROC_INST_ID_
) pti on hp.PROC_INST_ID_ = pti.PROC_INST_ID_
left join (
	select hv.PROC_INST_ID_,mp.wo_code,mp.wo_name,mp.wo_url from act_hi_varinst hv join act_ex_mapping mp 
	on hv.LONG_ = mp.ID 
	union
	select hv.PROC_INST_ID_,max(mp.wo_code),max(mp.wo_name),max(mp.wo_url) from act_hi_varinst hv join act_ex_mapping mp 
	on hv.TEXT_ = mp.wo_code where hv.NAME_ != '@woType' group by hv.PROC_INST_ID_
) hm on hm.PROC_INST_ID_ = hp.PROC_INST_ID_
left join act_id_user u on hp.START_USER_ID_ = u.ID_
left join act_ex_procinst ep on hp.PROC_INST_ID_ = ep.PROC_INST_ID;

-- oracle
CREATE OR REPLACE VIEW v_act_procinst AS 
select 

hp.*,
u.LAST_ as start_user_name,
pti.transactor as transactor,
pti.transactor_name as transactor_name,
pti.SUSPENSION_STATE_ as SUSPENSION_STATE_,
hm.WO_CODE as WO_CODE,
hm.WO_NAME as WO_NAME,
hm.WO_URL as WO_URL,
ep.status as status_

from act_hi_procinst hp 

left join (
	select 
		wm_concat(DISTINCT transactor) as transactor,
		wm_concat(DISTINCT transactor_name) as transactor_name,
		PROC_INST_ID_,
		max(SUSPENSION_STATE_) as SUSPENSION_STATE_
	from
	(
		select 
		
		decode(wm_concat(rt.ASSIGNEE_), null, wm_concat(ri.USER_ID_), wm_concat(rt.ASSIGNEE_)) as transactor,
		wm_concat(u.Last_) AS transactor_name,
		MAX(rt.PROC_INST_ID_) as PROC_INST_ID_,
		max(rt.SUSPENSION_STATE_) as SUSPENSION_STATE_
		
		from act_ru_task rt 
		left join act_ru_identitylink ri on ri.TASK_ID_ = rt.ID_ and ri.TYPE_ = 'candidate'
		left join act_id_user u ON
		decode(rt.ASSIGNEE_, null, ri.USER_ID_, rt.ASSIGNEE_) = u.ID_
		group by rt.ID_
	) rti group by rti.PROC_INST_ID_
) pti on hp.PROC_INST_ID_ = pti.PROC_INST_ID_
left join (
	select hv.PROC_INST_ID_,mp.wo_code,mp.wo_name,mp.wo_url from act_hi_varinst hv join act_ex_mapping mp 
	on hv.LONG_ = mp.ID 
	union
	select hv.PROC_INST_ID_,max(mp.wo_code),max(mp.wo_name),max(mp.wo_url) from act_hi_varinst hv join act_ex_mapping mp 
	on hv.TEXT_ = mp.wo_code where hv.NAME_ != '@woType' group by hv.PROC_INST_ID_
) hm on hm.PROC_INST_ID_ = hp.PROC_INST_ID_
left join act_id_user u on hp.START_USER_ID_ = u.ID_
left join act_ex_procinst ep on hp.PROC_INST_ID_ = ep.PROC_INST_ID;

