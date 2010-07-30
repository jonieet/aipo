/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2008 Aimluck,Inc.
 * http://aipostyle.com/
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.aimluck.eip.schedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cayenne.om.portlet.EipMFacility;
import com.aimluck.eip.cayenne.om.portlet.EipTSchedule;
import com.aimluck.eip.cayenne.om.portlet.EipTScheduleMap;
import com.aimluck.eip.cayenne.om.portlet.EipTTodo;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.facilities.util.FacilitiesUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.DatabaseOrmService;
import com.aimluck.eip.schedule.util.ScheduleUtils;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.todo.util.ToDoUtils;
import com.aimluck.eip.util.ALEipUtils;

/**
 * 週間スケジュール（グループ）の検索結果を管理するクラスです。
 * 
 */
public class ScheduleWeeklyGroupSelectData extends ScheduleWeeklySelectData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
      .getLogger(ScheduleWeeklyGroupSelectData.class.getName());

  /** <code>termmap</code> 期間スケジュールマップ */
  private Map termmap;

  /** <code>map</code> スケジュールマップ */
  private Map map;

  /** <code>members</code> 共有メンバー */
  private List members;

  /** <code>groups</code> グループ */
  private List groups;

  /** <code>userid</code> ログインユーザーID */
  private Integer userid;

  /** <code>myGroupURI</code> ポートレット MyGroup へのリンク */
  private String myGroupURI;

  /** <code>todomap</code> ToDo マップ */
  private Map todomap;

  /** ポートレット ID */
  private String portletId;

  /** <code>map</code> スケジュールMap（施設） */
  private Map facilitymap;

  private List facilityList;

  /** <code>hasAuthoritySelfInsert</code> アクセス権限 */
  private boolean hasAuthoritySelfInsert = false;

  /** <code>hasAuthorityFacilityInsert</code> アクセス権限 */
  private boolean hasAuthorityFacilityInsert = false;

  /** ログインユーザのスケジュールの上位表示フラグ名 */
  protected final String FLAG_CHANGE_TURN_STR = new StringBuffer().append(
      this.getClass().getName()).append("flagchangeturn").toString();

  /*
   * @see com.aimluck.eip.common.ALAbstractSelectData#init(com.aimluck.eip.modules.actions.common.ALAction,
   *      org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
   */
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    // スーパークラスのメソッドを呼び出す
    super.init(action, rundata, context);
    // 表示タイプの設定
    viewtype = "weekly-group";
    try {
      termmap = new LinkedHashMap();
      map = new LinkedHashMap();
      todomap = new LinkedHashMap();
      facilitymap = new LinkedHashMap();

      groups = ALEipUtils.getMyGroups(rundata);
      userid = Integer.valueOf(ALEipUtils.getUserId(rundata));
      String filter = ALEipUtils.getTemp(rundata, context, LIST_FILTER_STR);
      if (filter == null || filter.equals("")) {
        VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
        String groupName = portlet.getPortletConfig().getInitParameter(
            "p3a-group");
        if (groupName != null) {
          ALEipUtils.setTemp(rundata, context, LIST_FILTER_STR, groupName);
          ALEipUtils.setTemp(rundata, context, LIST_FILTER_TYPE_STR, "group");
        }
      }

      // ログインユーザのスケジュールを上位表示するかを確認する．
      String flag_changeturn = ALEipUtils.getTemp(rundata, context,
          FLAG_CHANGE_TURN_STR);
      if (flag_changeturn == null || flag_changeturn.equals("")) {
        VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
        String changeturnFlag = portlet.getPortletConfig().getInitParameter(
            "p3b-group");
        if (changeturnFlag != null) {
          ALEipUtils.setTemp(rundata, context, FLAG_CHANGE_TURN_STR,
              changeturnFlag);
        }
      }

      // アクセス権限
      ALAccessControlFactoryService aclservice = (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
          .getInstance())
          .getService(ALAccessControlFactoryService.SERVICE_NAME);
      ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

      hasAuthoritySelfInsert = aclhandler.hasAuthority(userid,
          ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF,
          ALAccessControlConstants.VALUE_ACL_INSERT);

      hasAuthorityFacilityInsert = aclhandler.hasAuthority(userid,
          ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_FACILITY,
          ALAccessControlConstants.VALUE_ACL_INSERT);

      // ポートレット MyGroup のへのリンクを取得する．
      myGroupURI = ScheduleUtils.getPortletURIinPersonalConfigPane(rundata,
          "MyGroup");
    } catch (Exception ex) {
      logger.error("Exception", ex);
    }
  }

  /*
   * @see com.aimluck.eip.common.ALAbstractSelectData#selectList(org.apache.turbine.util.RunData,
   *      org.apache.velocity.context.Context)
   */
  protected List selectList(RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    try {
      List list = dataContext.performQuery(getSelectQuery(rundata, context));

      if (viewTodo == 1) {
        // ToDO の読み込み
        loadTodo(rundata, context);
      }

      return ScheduleUtils.sortByDummySchedule(list);
    } catch (Exception e) {

      // TODO: エラー処理
      logger.error("[ScheduleWeeklyGroupSelectData]", e);
      throw new ALDBErrorException();

    }
  }

  /**
   * 検索条件を設定した SelectQuery を返します。
   * 
   * @param rundata
   * @param context
   * @return
   */
  protected SelectQuery getSelectQuery(RunData rundata, Context context) {
    SelectQuery query = new SelectQuery(EipTScheduleMap.class);

    // 終了日時
    Expression exp11 = ExpressionFactory.greaterOrEqualExp(
        EipTScheduleMap.EIP_TSCHEDULE_PROPERTY + "."
            + EipTSchedule.END_DATE_PROPERTY, getViewStart().getValue());
    // 開始日時
    Expression exp12 = ExpressionFactory.lessExp(
        EipTScheduleMap.EIP_TSCHEDULE_PROPERTY + "."
            + EipTSchedule.START_DATE_PROPERTY, getViewEndCrt().getValue());
    // 通常スケジュール
    Expression exp13 = ExpressionFactory.noMatchExp(
        EipTScheduleMap.EIP_TSCHEDULE_PROPERTY + "."
            + EipTSchedule.REPEAT_PATTERN_PROPERTY, "N");
    // 期間スケジュール
    Expression exp14 = ExpressionFactory.noMatchExp(
        EipTScheduleMap.EIP_TSCHEDULE_PROPERTY + "."
            + EipTSchedule.REPEAT_PATTERN_PROPERTY, "S");

    query.setQualifier((exp11.andExp(exp12)).orExp(exp13.andExp(exp14)));

    // 開始日時でソート
    query.addOrdering(EipTScheduleMap.EIP_TSCHEDULE_PROPERTY + "."
        + EipTSchedule.START_DATE_PROPERTY, true);

    return buildSelectQueryForFilter(query, rundata, context);
  }

  /*
   * @see com.aimluck.eip.common.ALAbstractSelectData#getResultData(java.lang.Object)
   */
  protected Object getResultData(Object obj) throws ALPageNotFoundException,
      ALDBErrorException {
    ScheduleResultData rd = new ScheduleResultData();
    rd.initField();
    try {
      EipTScheduleMap record = (EipTScheduleMap) obj;
      EipTSchedule schedule = record.getEipTSchedule();
      // スケジュールが棄却されている場合は表示しない
      if ("R".equals(record.getStatus()))
        return rd;

      SelectQuery mapquery = new SelectQuery(EipTScheduleMap.class);
      Expression mapexp1 = ExpressionFactory.matchExp(
          EipTScheduleMap.SCHEDULE_ID_PROPERTY, schedule.getScheduleId());
      mapquery.setQualifier(mapexp1);
      Expression mapexp2 = ExpressionFactory.matchExp(
          EipTScheduleMap.USER_ID_PROPERTY, userid);
      mapquery.andQualifier(mapexp2);

      List schedulemaps = dataContext.performQuery(mapquery);
      boolean is_member = (schedulemaps != null && schedulemaps.size() > 0) ? true
          : false;

      // Dummy スケジュールではない
      // 完全に隠す
      // 自ユーザー以外
      // 共有メンバーではない
      // オーナーではない
      if ((!"D".equals(record.getStatus()))
          && "P".equals(schedule.getPublicFlag())
          && (userid.intValue() != record.getUserId().intValue())
          && (userid.intValue() != schedule.getOwnerId().intValue())
          && !is_member)
        return rd;
      if ("C".equals(schedule.getPublicFlag())
          && (userid.intValue() != record.getUserId().intValue())
          && (userid.intValue() != schedule.getOwnerId().intValue())
          && !is_member) {
        // 名前
        rd.setName("非公開");
        // 仮スケジュールかどうか
        rd.setTmpreserve(false);
      } else {
        // 名前
        rd.setName(schedule.getName());
        // 仮スケジュールかどうか
        rd.setTmpreserve("T".equals(record.getStatus()));
      }
      // ID
      rd.setScheduleId(schedule.getScheduleId().intValue());
      // 親スケジュール ID
      rd.setParentId(schedule.getParentId().intValue());
      // 開始日時
      rd.setStartDate(schedule.getStartDate());
      // 終了日時
      rd.setEndDate(schedule.getEndDate());
      // 公開するかどうか
      rd.setPublic("O".equals(schedule.getPublicFlag()));
      // 非表示にするかどうか
      rd.setHidden("P".equals(schedule.getPublicFlag()));
      // ダミーか
      rd.setDummy("D".equals(record.getStatus()));
      // ログインユーザかどうか
      rd.setLoginuser(record.getUserId().intValue() == userid.intValue());
      // オーナーかどうか
      rd.setOwner(schedule.getOwnerId().intValue() == userid.intValue());
      // 共有メンバーかどうか
      rd.setMember(is_member);
      // 繰り返しパターン
      rd.setPattern(schedule.getRepeatPattern());

      // // 週間スケジュールコンテナを取得
      // ScheduleWeekContainer weekCon = (ScheduleWeekContainer) map
      // .get(new Integer(record.getUserId()));

      // ユーザもしくは設備の週間スケジュールコンテナを取得する．
      ScheduleWeekContainer weekCon = null;
      if (ScheduleUtils.SCHEDULEMAP_TYPE_USER.equals(record.getType())) {
        weekCon = (ScheduleWeekContainer) map.get(record.getUserId());
      } else {
        // if (ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY.equals(record.getType()))
        // の場合
        weekCon = (ScheduleWeekContainer) facilitymap.get(record.getUserId());
      }

      // 期間スケジュールの場合（「ログインユーザの期間スケジュール」もしくは「完全非公開以外の期間スケジュール」）
      if (rd.getPattern().equals("S")) {
        int stime = -(int) ((getViewStart().getValue().getTime() - rd
            .getStartDate().getValue().getTime()) / 86400000);
        int etime = -(int) ((getViewStart().getValue().getTime() - rd
            .getEndDate().getValue().getTime()) / 86400000);
        if (stime < 0) {
          stime = 0;
        }
        int count = stime;
        int col = etime - stime + 1;
        if (count + col > 7) {
          col = 7 - count;
        }
        rd.setRowspan(col);
        if (col > 0) {
          ArrayList terms = (ArrayList) termmap.get(record.getUserId());
          if (terms != null) {
            // 期間スケジュールを格納
            ScheduleUtils.addTermSchedule(terms, getViewStart().getValue(),
                count, rd);
          }
        }
        return rd;
      }
      // 週間種スケジュールコンテナに格納
      weekCon.addResultData(rd);
    } catch (Exception e) {

      // TODO: エラー処理
      logger.error("Exception", e);
      return null;
    }
    return rd;
  }

  /*
   * @see com.aimluck.eip.common.ALAbstractSelectData#buildCriteriaForFilter(org.apache.torque.util.Criteria,
   *      org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
   */
  protected SelectQuery buildSelectQueryForFilter(SelectQuery query,
      RunData rundata, Context context) {
    String filter = ALEipUtils.getTemp(rundata, context, LIST_FILTER_STR);
    String filter_type = ALEipUtils.getTemp(rundata, context,
        LIST_FILTER_TYPE_STR);
    String crt_key = null;
    Attributes map = getColumnMap();

    if (filter == null || filter_type == null || filter.equals("")) {
      // crt.add(EipTScheduleMapConstants.USER_ID,
      // ALEipUtils.getUserId(rundata));
      Expression exp1 = ExpressionFactory.matchExp(
          EipTScheduleMap.USER_ID_PROPERTY, userid);
      query.andQualifier(exp1);

      members = new ArrayList();
      members.add(ALEipUtils.getALEipUser(rundata));
      Calendar cal = Calendar.getInstance();
      cal.setTime(tmpCal.getTime());
      ScheduleWeekContainer week = new ScheduleWeekContainer();
      week.initField();
      week.setViewStartDate(cal);
      this.termmap.put(userid, new ArrayList());
      this.map.put(userid, week);
      this.todomap.put(userid, new ArrayList());
      return query;
    }

    crt_key = map.getValue(filter_type);
    if (crt_key == null)
      return query;

    // グループ名からユーザを取得
    List ulist = ALEipUtils.getUserIds(filter);

    // グループにユーザが存在しない場合はダミーユーザを設定し、検索します。
    // ダミーユーザーID = -1
    int size = ulist.size();
    if (size == 0) {
      ulist.add(Integer.valueOf(-1));
    } else {
      for (int i = 0; i < size; i++) {
        Integer id = (Integer) ulist.get(i);
        Calendar cal = Calendar.getInstance();
        cal.setTime(tmpCal.getTime());
        ScheduleWeekContainer week = new ScheduleWeekContainer();
        week.initField();
        week.setViewStartDate(cal);
        this.termmap.put(id, new ArrayList());
        this.map.put(id, week);
        this.todomap.put(id, new ArrayList());
      }
    }

    List facilityIds = null;

    if ("Facility".equals(filter)) {
      facilityIds = getFacilityIdAllList();
    } else {
      facilityIds = FacilitiesUtils.getFacilityIds(filter);
    }
    int f_size = facilityIds.size();
    if (f_size == 0) {
      facilityIds.add(Integer.valueOf(-1));
    } else {
      for (int i = 0; i < f_size; i++) {
        Integer id = (Integer) facilityIds.get(i);
        Calendar cal = Calendar.getInstance();
        cal.setTime(tmpCal.getTime());
        ScheduleWeekContainer week = new ScheduleWeekContainer();
        week.initField();
        week.setViewStartDate(cal);
        this.facilitymap.put(id, week);
      }
    }

    if ("Facility".equals(filter)) {
      Expression exp21 = ExpressionFactory.matchExp(
          EipTScheduleMap.TYPE_PROPERTY,
          ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY);
      Expression exp22 = ExpressionFactory.inExp(crt_key, facilityIds);

      query.andQualifier(exp21.andExp(exp22));
    } else {
      Expression exp11 = ExpressionFactory.matchExp(
          EipTScheduleMap.TYPE_PROPERTY, ScheduleUtils.SCHEDULEMAP_TYPE_USER);
      Expression exp12 = ExpressionFactory.inExp(crt_key, ulist);

      Expression exp21 = ExpressionFactory.matchExp(
          EipTScheduleMap.TYPE_PROPERTY,
          ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY);
      Expression exp22 = ExpressionFactory.inExp(crt_key, facilityIds);

      query.andQualifier((exp11.andExp(exp12)).orExp(exp21.andExp(exp22)));
    }
    members = ALEipUtils.getUsers(filter);
    String flag_changeturn = ALEipUtils.getTemp(rundata, context,
        FLAG_CHANGE_TURN_STR);
    if ("0".equals(flag_changeturn)) {
      // ログインユーザの行けジュールを一番上に表示させるため，
      // メンバリストの初めの要素にログインユーザを配置する．
      ALEipUser eipUser = null;
      int memberSize = members.size();
      for (int i = 0; i < memberSize; i++) {
        eipUser = (ALEipUser) members.get(i);
        if (eipUser.getUserId().getValue() == userid.intValue()) {
          members.remove(i);
          members.add(0, eipUser);
        }
      }
    }

    facilityList = FacilitiesUtils.getFacilityList(filter);

    current_filter = filter;
    current_filter_type = filter_type;
    return query;
  }

  private List getFacilityIdAllList() {
    List facilityIdAllList = new ArrayList();

    try {
      DataContext dataContext = DatabaseOrmService.getInstance()
          .getDataContext();
      SelectQuery query = new SelectQuery(EipMFacility.class);
      query.addCustomDbAttribute(EipMFacility.FACILITY_ID_PK_COLUMN);
      List aList = dataContext.performQuery(query);

      int size = aList.size();
      for (int i = 0; i < size; i++) {
        DataRow dataRow = (DataRow) aList.get(i);
        facilityIdAllList.add((Integer) ALEipUtils.getObjFromDataRow(dataRow,
            EipMFacility.FACILITY_ID_PK_COLUMN));
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
    }
    return facilityIdAllList;
  }

  /*
   * @see com.aimluck.eip.common.ALAbstractSelectData#getColumnMap()
   */
  protected Attributes getColumnMap() {
    Attributes map = new Attributes();
    map.putValue("group", EipTScheduleMap.USER_ID_PROPERTY);
    return map;
  }

  public void loadTodo(RunData rundata, Context context) {
    try {
      SelectQuery query = getSelectQueryForTodo(rundata, context);
      List todos = dataContext.performQuery(query);

      int todossize = todos.size();
      for (int i = 0; i < todossize; i++) {
        EipTTodo record = (EipTTodo) todos.get(i);
        ScheduleToDoResultData rd = new ScheduleToDoResultData();
        rd.initField();

        // ポートレット ToDo のへのリンクを取得する．
        String todo_url = "";
        if (userid.intValue() == record.getTurbineUser().getUserId().intValue()) {
          todo_url = ScheduleUtils.getPortletURItoTodoDetailPane(rundata,
              "ToDo", record.getTodoId().longValue(), portletId);
        } else {
          todo_url = ScheduleUtils.getPortletURItoTodoPublicDetailPane(rundata,
              "ToDo", record.getTodoId().longValue(), portletId);
        }

        rd.setTodoId(record.getTodoId().intValue());
        rd.setTodoName(record.getTodoName());
        rd.setUserId(record.getTurbineUser().getUserId().intValue());
        rd.setStartDate(record.getStartDate());
        rd.setEndDate(record.getEndDate());
        rd.setTodoUrl(todo_url);
        // 公開/非公開を設定する．
        rd.setPublicFlag("T".equals(record.getPublicFlag()));

        int stime;
        if (ScheduleUtils.equalsToDate(ToDoUtils.getEmptyDate(), rd
            .getStartDate().getValue(), false)) {
          stime = 0;
        } else {
          stime = -(int) ((getViewStart().getValue().getTime() - rd
              .getStartDate().getValue().getTime()) / 86400000);
        }
        int etime = -(int) ((getViewStart().getValue().getTime() - rd
            .getEndDate().getValue().getTime()) / 86400000);
        if (stime < 0) {
          stime = 0;
        }
        int count = stime;
        int col = etime - stime + 1;
        // 行をはみ出す場合
        if (count + col > 7) {
          col = 7 - count;
        }
        // rowspan を設定
        rd.setRowspan(col);
        if (col > 0) {
          ArrayList usertodos1 = (ArrayList) todomap.get(record
              .getTurbineUser().getUserId());
          if (usertodos1 != null) {
            // ToDo を格納
            ScheduleUtils.addToDo(usertodos1, getViewStart().getValue(), count,
                rd);
          }
        }
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return;
    }
  }

  private SelectQuery getSelectQueryForTodo(RunData rundata, Context context) {
    SelectQuery query = new SelectQuery(EipTTodo.class);
    Expression exp1 = ExpressionFactory.noMatchExp(EipTTodo.STATE_PROPERTY,
        Short.valueOf((short) 100));
    query.setQualifier(exp1);

    Expression exp01 = ExpressionFactory.matchDbExp(
        TurbineUser.USER_ID_PK_COLUMN, userid);
    Expression exp02 = ExpressionFactory.noMatchDbExp(
        TurbineUser.USER_ID_PK_COLUMN, userid);
    Expression exp03 = ExpressionFactory.matchExp(
        EipTTodo.PUBLIC_FLAG_PROPERTY, "T");
    Expression exp04 = ExpressionFactory.matchExp(
        EipTTodo.ADDON_SCHEDULE_FLG_PROPERTY, "T");
    query.andQualifier(exp01.orExp(exp02.andExp(exp03)).andExp(exp04));

    // 終了日時
    Expression exp11 = ExpressionFactory.greaterOrEqualExp(
        EipTTodo.END_DATE_PROPERTY, getViewStart().getValue());
    // 開始日時
    Expression exp12 = ExpressionFactory.lessOrEqualExp(
        EipTTodo.START_DATE_PROPERTY, getViewEnd().getValue());

    // 開始日時のみ指定されている ToDo を検索
    Expression exp21 = ExpressionFactory.lessOrEqualExp(
        EipTTodo.START_DATE_PROPERTY, getViewEnd().getValue());
    Expression exp22 = ExpressionFactory.matchExp(EipTTodo.END_DATE_PROPERTY,
        ToDoUtils.getEmptyDate());

    // 終了日時のみ指定されている ToDo を検索
    Expression exp31 = ExpressionFactory.greaterOrEqualExp(
        EipTTodo.END_DATE_PROPERTY, getViewStart().getValue());
    Expression exp32 = ExpressionFactory.matchExp(EipTTodo.START_DATE_PROPERTY,
        ToDoUtils.getEmptyDate());

    query.andQualifier((exp11.andExp(exp12)).orExp(exp21.andExp(exp22)).orExp(
        exp31.andExp(exp32)));
    return query;
  }

  /**
   * 週間スケジュールコンテナを取得します。
   * 
   * @param id
   * @return
   */
  public ScheduleWeekContainer getContainer(long id) {
    return (ScheduleWeekContainer) map.get(Integer.valueOf((int) id));
  }

  /**
   * 共有メンバーを取得します。
   * 
   * @return
   */
  public List getMemberList() {
    return members;
  }

  /**
   * 部署マップを取得します。
   * 
   * @return
   */
  public Map getPostMap() {
    return ALEipManager.getInstance().getPostMap();
  }

  /**
   * グループリストを取得します。
   * 
   * @return
   */
  public List getGroupList() {
    return groups;
  }

  /**
   * 
   * @param id
   * @return
   */
  public boolean isMatch(long id) {
    return userid.intValue() == (int) id;
  }

  /**
   * ポートレット MyGroup へのリンクを取得する．
   * 
   * @return
   */
  public String getMyGroupURI() {
    return myGroupURI;
  }

  /**
   * 期間スケジュールコンテナを取得します。
   * 
   * @param id
   * @return
   */
  public List getTermContainer(long id) {
    return (List) termmap.get(Integer.valueOf((int) id));
  }

  /**
   * ToDo コンテナを取得します。
   * 
   * @param id
   * @return
   */
  public List getToDoContainer(long id) {
    return (List) todomap.get(Integer.valueOf((int) id));
  }

  public void setPortletId(String id) {
    portletId = id;
  }

  public List getFacilityList() {
    return facilityList;
  }

  /**
   * 施設の週間スケジュールコンテナを取得します。
   * 
   * @param id
   * @return
   */
  public ScheduleWeekContainer getFacilityContainer(long id) {
    return (ScheduleWeekContainer) facilitymap.get(Integer.valueOf((int) id));
  }

  /**
   * ログインユーザの ID を取得する．
   * 
   * @return
   */
  public long getUserId() {
    return userid.longValue();
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限の機能名を返します。
   * 
   * @return
   */
  public String getAclPortletFeature() {
    return ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_OTHER;
  }

  public boolean hasAuthoritySelfInsert() {
    return hasAuthoritySelfInsert;
  }

  public boolean hasAuthorityFacilityInsert() {
    return hasAuthorityFacilityInsert;
  }

}
