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
import java.util.List;
import java.util.Map;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.util.ALEipUtils;

/**
 * スケジュールのフォームデータを管理するクラスです。
 * 
 */
public class CellScheduleFormGroupForSelectData extends ALAbstractFormData {

  /** <code>login_user</code> ログインユーザー */
  private ALEipUser login_user;

  /** <code>groups</code> グループ */
  private List groups;

  /**
   * フォームを表示します。
   * 
   * @param action
   * @param rundata
   * @param context
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  public boolean doViewForm(ALAction action, RunData rundata, Context context) {
    try {
      init(action, rundata, context);

      action.setMode(ALEipConstants.MODE_NEW_FORM);
      setMode(action.getMode());
      ArrayList msgList = new ArrayList();
      action.setResultData(this);
      action.addErrorMessages(msgList);
      action.putData(rundata, context);
      return true;
    } catch (ALPageNotFoundException e) {
      ALEipUtils.redirectPageNotFound(rundata);
      return false;
    } catch (ALDBErrorException e) {
      ALEipUtils.redirectDBError(rundata);
      return false;
    }

  }

  /*
   * @see com.aimluck.eip.common.ALAbstractFormData#init(com.aimluck.eip.modules.actions.common.ALAction,
   *      org.apache.turbine.util.RunData,
   *      org.apache.velocity.context.Context)
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    login_user = ALEipUtils.getALEipUser(rundata);

    groups = ALEipUtils.getMyGroups(rundata);

    // スーパークラスのメソッドを呼び出す。
    super.init(action, rundata, context);
  }

  /**
   * パラメータを読み込みます。
   * 
   * @param rundata
   * @param context
   */
  public void loadParameters(RunData rundata, Context context) {
  }

  /*
   * @see com.aimluck.eip.common.ALData#initField()
   */
  public void initField() {
  }

  /*
   * @see com.aimluck.eip.common.ALAbstractFormData#loadFormData(org.apache.turbine.util.RunData,
   *      org.apache.velocity.context.Context, java.util.ArrayList)
   */
  @Override
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {

    return true;
  }

  /**
   * 指定したグループ名のユーザーを取得します。
   * 
   * @param groupname
   * @return
   */
  public List getUsers(String groupname) {
    return ALEipUtils.getUsers(groupname);
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
   * ログインユーザを取得します。
   * 
   * @return
   */
  public ALEipUser getLoginUser() {
    return login_user;
  }

  /**
   * グループリストを取得します。
   * 
   * @return
   */
  public List getGroupList() {
    return groups;
  }

  @Override
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    // TODO 自動生成されたメソッド・スタブ
    return false;
  }

  @Override
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    // TODO 自動生成されたメソッド・スタブ
    return false;
  }

  @Override
  protected void setValidator() throws ALPageNotFoundException,
      ALDBErrorException {
    // TODO 自動生成されたメソッド・スタブ

  }

  @Override
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    // TODO 自動生成されたメソッド・スタブ
    return false;
  }

  @Override
  protected boolean validate(List<String> msgList) throws ALPageNotFoundException,
      ALDBErrorException {
    // TODO 自動生成されたメソッド・スタブ
    return true;
  }
}
