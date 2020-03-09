-- mysql
DROP VIEW IF EXISTS	v_act_procinst;
create view v_act_procinst as
select 

hp.*,
rti.transactor as transactor,
rm.WO_CODE as WO_CODE,
rm.WO_NAME as WO_NAME,
rm.WO_URL as WO_URL

from act_hi_procinst hp 

left join (

	select 

	rt.PROC_INST_ID_,
	(
		case 
			when ISNULL(GROUP_CONCAT(rt.ASSIGNEE_)) then GROUP_CONCAT(ri.USER_ID_)
			else GROUP_CONCAT(rt.ASSIGNEE_)
		end
	) as transactor
	
	from act_ru_identitylink ri join act_ru_task rt on ri.TASK_ID_ = rt.ID_ WHERE ri.TYPE_ = 'candidate' group by rt.PROC_INST_ID_
			
) rti on hp.PROC_INST_ID_ = rti.PROC_INST_ID_

left join (
	select rv.PROC_INST_ID_,mp.wo_code,mp.wo_name,mp.wo_url from act_ru_variable rv join act_ex_mapping mp 
	on rv.LONG_ = mp.ID 
	union
	select rv.PROC_INST_ID_,max(mp.wo_code),max(mp.wo_name),max(mp.wo_url) from act_ru_variable rv join act_ex_mapping mp 
	on rv.TEXT_ = mp.wo_code where rv.NAME_ != '@woType' group by rv.PROC_INST_ID_
) rm on rm.PROC_INST_ID_ = hp.PROC_INST_ID_;

-- oracle
CREATE OR REPLACE VIEW v_act_procinst AS 
select 

hp.*,
rti.transactor as transactor,
rm.WO_CODE as WO_CODE,
rm.WO_NAME as WO_NAME,
rm.WO_URL as WO_URL

from act_hi_procinst hp 

left join (

	select 

	rt.PROC_INST_ID_,
	(
		case 
			when wm_concat(rt.ASSIGNEE_) is null then wm_concat(ri.USER_ID_)
			else wm_concat(rt.ASSIGNEE_)
		end
	) as transactor
	
	from act_ru_identitylink ri join act_ru_task rt on ri.TASK_ID_ = rt.ID_ WHERE ri.TYPE_ = 'candidate' group by rt.PROC_INST_ID_
			
) rti on hp.PROC_INST_ID_ = rti.PROC_INST_ID_

left join (
	select rv.PROC_INST_ID_,mp.wo_code,mp.wo_name,mp.wo_url from act_ru_variable rv join act_ex_mapping mp 
	on rv.LONG_ = mp.ID 
	union
	select rv.PROC_INST_ID_,max(mp.wo_code),max(mp.wo_name),max(mp.wo_url) from act_ru_variable rv join act_ex_mapping mp 
	on rv.TEXT_ = mp.wo_code where rv.NAME_ != '@woType' group by rv.PROC_INST_ID_
) rm on rm.PROC_INST_ID_ = hp.PROC_INST_ID_;