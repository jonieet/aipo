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
package com.aimluck.eip.webmail;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import javax.mail.Message;
import javax.mail.internet.MimeUtility;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.jetspeed.om.security.JetspeedUser;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.rundata.JetspeedRunData;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cayenne.om.portlet.EipMMailAccount;
import com.aimluck.eip.cayenne.om.portlet.EipTMail;
import com.aimluck.eip.cayenne.om.portlet.EipTMailFolder;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.mail.ALFolder;
import com.aimluck.eip.mail.ALLocalMailMessage;
import com.aimluck.eip.mail.ALMailFactoryService;
import com.aimluck.eip.mail.ALMailHandler;
import com.aimluck.eip.mail.ALPop3MailReceiveThread;
import com.aimluck.eip.mail.util.ALMailUtils;
import com.aimluck.eip.mail.util.UnicodeCorrecter;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.DatabaseOrmService;
import com.aimluck.eip.util.ALCommonUtils;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.webmail.beans.WebmailAccountLiteBean;
import com.aimluck.eip.webmail.util.WebMailUtils;
import com.sk_jp.mail.MailUtility;

/**
 * Webメール検索データを管理するためのクラスです。 <br />
 */
public class WebMailSelectData extends ALAbstractSelectData {
  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
      .getLogger(WebMailSelectData.class.getName());

  /** 現在選択されているタブ (＝受信メール or 送信メール) */
  private String currentTab = null;

  JetspeedUser user = null;

  /** ユーザーID */
  private int userId = -1;

  /** アカウントID */
  private int accountId = -1;

  /** フォルダID */
  private int folderId = -1;

  /** フォルダに対する未読メール数のマップ */
  private Map<Integer,Integer> unreadMailSumMap;

  /** 最終受信日 */
  private String finalAccessDateStr = null;

  private String org_id;

  private DataContext dataContext;

  /** 受信トレイと送信トレイ */
  private ALFolder folder;

  /** 選択されたフォルダ */
  private EipTMailFolder selectedFolder;

  /** メールアカウント一覧 */
  private ArrayList mailAccountList;

  /** メールフォルダ一覧 */
  private List<EipTMailFolder> mailFolderList;

  /**
   *
   * @param action
   * @param rundata
   * @param context
   * @see com.aimluck.eip.common.ALAbstractSelectData#init(com.aimluck.eip.modules.actions.common.ALAction,
   *      org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
   */
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    String sort = ALEipUtils.getTemp(rundata, context, LIST_SORT_STR);
    if (sort == null || sort.equals("")) {
      ALEipUtils.setTemp(rundata, context, LIST_SORT_STR, ALEipUtils
          .getPortlet(rundata, context).getPortletConfig().getInitParameter(
              "p2a-sort"));
    }

    org_id = DatabaseOrmService.getInstance().getOrgId(rundata);
    dataContext = DatabaseOrmService.getInstance().getDataContext();
    userId = ALEipUtils.getUserId(rundata);
    user = (JetspeedUser) ((JetspeedRunData) rundata).getUser();

    String tabParam = rundata.getParameters().getString("tab");
    currentTab = ALEipUtils.getTemp(rundata, context, "tab");
    if (tabParam == null && currentTab == null) {
      ALEipUtils.setTemp(rundata, context, "tab", WebMailUtils.TAB_RECEIVE);
      currentTab = WebMailUtils.TAB_RECEIVE;
    } else if (tabParam != null) {
      ALEipUtils.setTemp(rundata, context, "tab", tabParam);
      currentTab = tabParam;
    }

    String tmpAccoundId = ALEipUtils.getTemp(rundata, context,
        WebMailUtils.ACCOUNT_ID);
    if (tmpAccoundId == null || "".equals(tmpAccoundId)) {
      ALEipUtils.setTemp(rundata, context, WebMailUtils.ACCOUNT_ID, ALEipUtils
          .getPortlet(rundata, context).getPortletConfig().getInitParameter(
              "p3a-accounts"));
    }

    // 自ポートレットからのリクエストであれば、パラメータを展開しセッションに保存する。
    if (ALEipUtils.isMatch(rundata, context)) {
      // アカウントID
      if (rundata.getParameters().containsKey(WebMailUtils.ACCOUNT_ID)) {
        ALEipUtils.setTemp(rundata, context, WebMailUtils.ACCOUNT_ID, rundata
            .getParameters().getString(WebMailUtils.ACCOUNT_ID));
      }

      // フォルダID
      if (rundata.getParameters().containsKey(WebMailUtils.FOLDER_ID)) {
        ALEipUtils.setTemp(rundata, context, WebMailUtils.FOLDER_ID, rundata
            .getParameters().getString(WebMailUtils.FOLDER_ID));
      }
    }

    try {
      accountId = Integer.parseInt(ALEipUtils.getTemp(rundata, context,
          WebMailUtils.ACCOUNT_ID));
    } catch (Exception e) {
      accountId = 0;
    }

    try {
      folderId = Integer.parseInt(ALEipUtils.getTemp(rundata, context,
          WebMailUtils.FOLDER_ID));
    } catch (Exception e) {
      folderId = 0;
    }

    // アカウントIDが取得できなかったとき、デフォルトのアカウントIDを取得する
    if (accountId == 0) {
      try {
        Expression exp = ExpressionFactory.matchExp(
            EipMMailAccount.USER_ID_PROPERTY, userId);
        SelectQuery query = new SelectQuery(EipMMailAccount.class, exp);

        query.addCustomDbAttribute(EipMMailAccount.ACCOUNT_ID_PK_COLUMN);
        List accounts = dataContext.performQuery(query);
        if (accounts != null && accounts.size() > 0) {
          DataRow dataRow = (DataRow) accounts.get(0);
          accountId = (Integer) dataRow
              .get(EipMMailAccount.ACCOUNT_ID_PK_COLUMN);
          ALEipUtils.setTemp(rundata, context, WebMailUtils.ACCOUNT_ID, Integer
              .toString(accountId));
        } else {
          // アカウントが一つも見つからなかった
          return;
        }
      } catch (Exception e) {
      }
    }

    // アカウントを取得
    EipMMailAccount account = ALMailUtils.getMailAccount(org_id, userId,
        accountId);

    // 現在選択中のフォルダを取得
    selectedFolder = WebMailUtils.getEipTMailFolder(account, String
        .valueOf(folderId));

    // フォルダが取得できなかったとき、アカウントに紐付いたデフォルトのフォルダIDを取得する
    if (selectedFolder == null) {
      folderId = account.getDefaultFolderId();

      // セッションにセット
      ALEipUtils.setTemp(rundata, context, WebMailUtils.FOLDER_ID, String
          .valueOf(folderId));

      // 再取得
      selectedFolder = WebMailUtils.getEipTMailFolder(account, String
          .valueOf(folderId));
    }

    // フォルダリストを取得
    mailFolderList = ALMailUtils.getEipTMailFolderAll(account);

    // 現在選択しているタブが受信トレイか送信トレイか
    if (accountId > 0) {
      int type_mail = (WebMailUtils.TAB_RECEIVE.equals(currentTab)) ? ALFolder.TYPE_RECEIVE
          : ALFolder.TYPE_SEND;
      ALMailHandler handler = ALMailFactoryService.getInstance()
          .getMailHandler();
      folder = handler.getALFolder(type_mail, org_id, userId, Integer
          .valueOf(accountId));
      folder.setRowsNum(super.getRowsNum());
    }

    super.init(action, rundata, context);

    // ソート対象が日時だった場合、ソート順を逆にする．
    if ("date".equals(ALEipUtils.getTemp(rundata, context, LIST_SORT_STR))) {
      String sort_type = ALEipUtils.getTemp(rundata, context,
          LIST_SORT_TYPE_STR);
      if (sort_type == null || sort_type.equals("")) {
        ALEipUtils.setTemp(rundata, context, LIST_SORT_TYPE_STR,
            ALEipConstants.LIST_SORT_TYPE_DESC);
      }
    }
  }

  /**
   *
   * @param rundata
   * @param context
   */
  public void loadMailAccountList(RunData rundata, Context context) {
    try {
      // メールアカウント一覧
      mailAccountList = new ArrayList();

      List aList = WebMailUtils.getMailAccountNameList(ALEipUtils
          .getUserId(rundata));

      if (aList == null)
        return;

      WebmailAccountLiteBean bean = null;
      DataRow dataRow = null;
      Iterator iter = aList.iterator();
      while (iter.hasNext()) {
        dataRow = (DataRow) iter.next();
        bean = new WebmailAccountLiteBean();
        bean.initField();
        bean.setAccountId(((Integer) ALEipUtils.getObjFromDataRow(dataRow,
            EipMMailAccount.ACCOUNT_ID_PK_COLUMN)).intValue());
        bean.setAccountName((String) ALEipUtils.getObjFromDataRow(dataRow,
            EipMMailAccount.ACCOUNT_NAME_COLUMN));
        mailAccountList.add(bean);
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
    }
  }

  /**
   * メールの一覧を取得する． <BR>
   *
   * @see com.aimluck.eip.common.ALAbstractSelectData#selectList(org.apache.turbine.util.RunData,
   *      org.apache.velocity.context.Context)
   */
  protected List selectList(RunData rundata, Context context) {
    try {
      if (folder == null) {
        return null;
      }

      // フォルダごとの未読メール数を取得
      // ・フォルダの切り替え、受信送信タブの移動、ソート時には未読メール数をセッションから取得する
      // ・メールのフォルダ間移動、メール詳細画面を出した後は未読メール数をデータベースから取得する
      // ・セッションが空の場合は未読メール数をデータベースから取得する
      String unreadMailSumMapString = ALEipUtils.getTemp(rundata, context,
          "unreadmailsummap");
      if ((rundata.getParameters().containsKey("noupdateunread")
          || rundata.getParameters().containsKey("sort") || rundata
          .getParameters().containsKey("tab"))
          && unreadMailSumMapString != null
          && !rundata.getParameters().containsKey("updateunread")) {
        // セッションから得た文字列をHashMapに再構成
        unreadMailSumMap = WebMailUtils
            .getUnreadMailSumMapFromString(unreadMailSumMapString);
      } else {
        // セッションが空か、パラメータが指定されていなければ取得しなおす
        unreadMailSumMap = WebMailUtils.getUnreadMailNumberMap(rundata, userId,
            accountId);
      }

      // セッションに保存
      ALEipUtils.setTemp(rundata, context, "unreadmailsummap", unreadMailSumMap
          .toString());

      return folder.getIndexRows(rundata, context);
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * @see com.aimluck.eip.common.ALAbstractSelectData#selectDetail(org.apache.turbine.util.RunData,
   *      org.apache.velocity.context.Context)
   */
  protected Object selectDetail(RunData rundata, Context context) {
    String mailid = ALEipUtils.getTemp(rundata, context,
        ALEipConstants.ENTITY_ID);

    if (mailid == null || Integer.valueOf(mailid) == null) {
      // Mail IDが空の場合
      logger.debug("[Mail] Empty ID...");
      return null;
    }
    return folder.getMail(Integer.valueOf(mailid));
  }

  /**
   * ResultDataを取得する（メールの一覧） <BR>
   *
   * @see com.aimluck.eip.common.ALAbstractSelectData#getResultData(java.lang.Object)
   */
  protected Object getResultData(Object obj) {
    DataRow dataRow = (DataRow) obj;

    WebMailIndexRowResultData rd = new WebMailIndexRowResultData();
    rd.initField();

    rd.setMailId(((Integer) ALEipUtils.getObjFromDataRow(dataRow,
        EipTMail.MAIL_ID_PK_COLUMN)).toString());

    String isRead = (String) ALEipUtils.getObjFromDataRow(dataRow,
        EipTMail.READ_FLG_COLUMN);
    if ("T".equals(isRead)) {
      rd.setReadImage("themes/"+getTheme()+"/images/icon/webmail_readmail.gif");
      rd.setReadImageDescription("既読");
    } else {
      rd.setReadImage("themes/"+getTheme()+"/images/icon/webmail_unreadmail.gif");
      rd.setReadImageDescription("未読");
    }

    String s = (String) ALEipUtils.getObjFromDataRow(dataRow,
        EipTMail.SUBJECT_COLUMN);
    try {
      s = MimeUtility.decodeText(MimeUtility.unfold(s));
      s = UnicodeCorrecter.correctToCP932(MailUtility.decodeText(s));
      rd.setSubject(ALCommonUtils.compressString(s, getStrLength()));
    } catch (UnsupportedEncodingException unsupportedencodingexception) {
      rd.setSubject(ALCommonUtils.compressString(MailUtility
          .decodeText((String) ALEipUtils.getObjFromDataRow(dataRow,
              EipTMail.SUBJECT_COLUMN)), getStrLength()));
    }

    rd.setPerson(MailUtility.decodeText((String) ALEipUtils.getObjFromDataRow(
        dataRow, EipTMail.PERSON_COLUMN)));

    rd.setDate((Date) ALEipUtils.getObjFromDataRow(dataRow,
        EipTMail.EVENT_DATE_COLUMN));
    rd.setFileVolume(((Integer) ALEipUtils.getObjFromDataRow(dataRow,
        EipTMail.FILE_VOLUME_COLUMN)).toString());

    boolean hasAttachments = ("T".equals((String) ALEipUtils.getObjFromDataRow(
        dataRow, EipTMail.HAS_FILES_COLUMN)));

    if (hasAttachments) {
      rd.setWithFilesImage("images/webmail/webmail_withfiles.gif");
      rd.setWithFilesImageDescription("添付有");
    }
    rd.hasAttachments(hasAttachments);

    return rd;
  }

  /**
   * @see com.aimluck.eip.common.ALAbstractSelectData#getResultDataDetail(java.lang.Object)
   */
  protected Object getResultDataDetail(Object obj) {
    WebMailResultData rd = null;
    try {
      ALLocalMailMessage msg = (ALLocalMailMessage) obj;

      String date = "";

      Date sentDate = msg.getSentDate();
      if (sentDate == null) {
        date = "Unknown";
      } else {
        date = ALMailUtils.translateDate(sentDate);
      }
      rd = new WebMailResultData();
      rd.initField();
      rd.setHeaders(msg.getHeaderArray());
      rd.setSubject(msg.getSubject());
      rd.setFrom(ALMailUtils.getAddressString(msg.getFrom()));
      rd.setTo(ALMailUtils.getAddressString(msg
          .getRecipients(Message.RecipientType.TO)));
      rd.setDate(date);
      // int length = 0;
      // String[] bodyTexts = null;

      // if (msg.isHtmlMail()) {
      // if (originalBodyTexts == null) {
      // length = 2;
      // bodyTexts = new String[length];
      // bodyTexts[0] = "このメールは HTML メールです。";
      // bodyTexts[1] = "本文は、下記の添付ファイル「message番号.html」を参照ください。";
      // } else {
      // length = originalBodyTexts.length + 4;
      // bodyTexts = new String[length];
      // bodyTexts[0] = "このメールは HTML メールです。";
      // bodyTexts[1] = "HTML メールの本文は、下記の添付ファイル「message番号.html」を参照ください。";
      // bodyTexts[2] = "HTML メールに含まれている本文のみを以下に抜き出して表示します。";
      // bodyTexts[3] = "-----------------------------";
      // for (int i = 4; i < length; i++) {
      // bodyTexts[i] = originalBodyTexts[i - 4];
      // }
      // }
      // } else {
      // bodyTexts = originalBodyTexts;
      // }
      //
      // bodyTexts = originalBodyTexts;

      rd.setBody(msg.getBodyText());
      rd.setAttachmentFileNames(msg.getAttachmentFileNameArray());
    } catch (Exception e) {
      logger.error("Exception", e);
    }
    return rd;
  }

  /**
   * 現在選択されているタブを取得します。 <BR>
   *
   * @return
   */
  public String getCurrentTab() {
    return currentTab;
  }

  /**
   * 現在のアカウントが持つメールフォルダを取得します。
   *
   * @return
   */
  public List getFolderList() {
    return mailFolderList;
  }

  /**
   * @return
   * @see com.aimluck.eip.common.ALAbstractSelectData#getColumnMap()
   */
  protected Attributes getColumnMap() {
    return null;
  }

  /**
   *
   * @return
   */
  public List getMailAccountList() {
    return mailAccountList;
  }

  /**
   * 現在選択中のアカウントIDを取得します。
   *
   * @return
   */
  public int getAccountId() {
    return accountId;
  }

  /**
   * 現在選択中のフォルダIDを取得します。
   *
   * @return
   */
  public int getFolderId() {
    return folderId;
  }

  /**
   * 現在選択中のフォルダを取得します。
   *
   * @return
   */
  public EipTMailFolder getSelectedFolder() {
    return selectedFolder;
  }

  public int getNewMailSum() {
    try {
      return WebMailUtils.getNewMailNumThread(org_id, user, accountId);
    } catch (Exception ex) {
      return 0;
    }
  }

  /**
   * フォルダ別未読メール数を取得する。
   *
   * @return
   */
  public int getUnReadMailSumByFolderId(int folder_id) {
    return (Integer) unreadMailSumMap.get(folder_id);
  }

  public String getFinalAccessDate() {
    return finalAccessDateStr;
  }

  /**
   * 表示する項目数を取得します。
   *
   * @return
   */
  public int getRowsNum() {
    return folder.getRowsNum();
  }

  /**
   * 総件数を取得します。
   *
   * @return
   */
  public int getCount() {
    return folder.getCount();
  }

  /**
   * 総ページ数を取得します。
   *
   * @return
   */
  public int getPagesNum() {
    return folder.getPagesNum();
  }

  /**
   * 現在表示されているページを取得します。
   *
   * @return
   */
  public int getCurrentPage() {
    return folder.getCurrentPage();
  }

  /**
   *
   * @return
   */
  public String getCurrentSort() {
    return folder.getCurrentSort();
  }

  /**
   *
   * @return
   */
  public String getCurrentSortType() {
    return folder.getCurrentSortType();
  }

  /**
   * @return
   */
  public int getStart() {
    return folder.getStart();
  }

  /**
   *
   * @param id
   * @return
   */
  public boolean isMatch(int id1, long id2) {
    return id1 == (int) id2;
  }

  public String getStatStr() {
    return ALPop3MailReceiveThread.getReceiveMailResultStr(user, accountId);
  }
}
