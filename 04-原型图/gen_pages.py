#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 design/pages 下的可交互 HTML 原型。共用 ../mock.css 与 ../icons.svg。"""
import os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "pages")
os.makedirs(OUT, exist_ok=True)
APP = "云栖露营地"

# ---------------- 导航配置（按角色） ----------------
NAV_MANAGER = [
    ("工作台", [("02_dashboard.html", "经营看板", "")]),
    ("营地资源", [("06_site_admin.html", "营区与营位", ""), ("07_price.html", "价格与停用", "")]),
    ("预约业务", [("09_booking_list.html", "预约管理", "6"), ("10_booking_calendar.html", "预约日历", ""),
                  ("11_arrival.html", "入住核验", "3"), ("12_checkout.html", "退营结算", "2")]),
    ("物资租赁", [("13_goods.html", "物资档案", ""), ("14_picklist.html", "备货与发放", "4"),
                  ("15_combo.html", "营位物资联合可用性", "")]),
    ("安全运营", [("16_patrol.html", "巡检任务", ""), ("17_event.html", "隐患与事件", "1")]),
    ("客户服务", [("18_review.html", "评价与黑名单", "")]),
    ("配置", [("19_rule.html", "规则配置", ""), ("20_system.html", "用户与角色", ""),
              ("21_message.html", "消息中心", "3"), ("22_profile.html", "个人中心", "")]),
]
NAV_RECEPTION = [
    ("工作台", [("02_dashboard.html", "今日值班", "")]),
    ("预约业务", [("09_booking_list.html", "预约管理", "6"), ("10_booking_calendar.html", "预约日历", ""),
                  ("11_arrival.html", "入住核验", "3"), ("12_checkout.html", "退营结算", "2")]),
    ("客户", [("18_review.html", "评价与黑名单", ""), ("17_event.html", "事件登记", "")]),
    ("配置", [("21_message.html", "消息中心", "2"), ("22_profile.html", "个人中心", "")]),
]
NAV_WAREHOUSE = [
    ("工作台", [("02_dashboard.html", "仓储看板", "")]),
    ("物资", [("13_goods.html", "物资档案", ""), ("14_picklist.html", "备货与发放", "4"),
              ("15_combo.html", "物资可用性校验", "")]),
    ("配置", [("21_message.html", "消息中心", "2"), ("22_profile.html", "个人中心", "")]),
]
NAV_ADMIN = [
    ("工作台", [("02_dashboard.html", "经营看板", "")]),
    ("系统", [("20_system.html", "用户与角色", ""), ("19_rule.html", "规则配置", "")]),
    ("配置", [("21_message.html", "消息中心", "3"), ("22_profile.html", "个人中心", "")]),
]
NAVS = {"manager": NAV_MANAGER, "reception": NAV_RECEPTION, "warehouse": NAV_WAREHOUSE, "admin": NAV_ADMIN}
ROLE_LABEL = {"manager": "运营经理", "reception": "前台人员", "warehouse": "仓库人员", "admin": "系统管理员", "guest": "游客"}
ROLE_AVATAR = {"manager": "顾", "reception": "林", "warehouse": "唐", "admin": "系", "guest": "客"}
ROLE_SIDE = {"manager": "#1F2937", "reception": "#24455C", "warehouse": "#2A504A", "admin": "#1B2430"}


def icon(name, size=18):
    return ('<svg width="%d" height="%d" aria-hidden="true"><use href="../icons.svg#i-%s"></use></svg>'
            % (size, size, name))


def nav(groups, active):
    buf = ['<nav class="nav">']
    for g, items in groups:
        buf.append('<div class="group">%s</div>' % g)
        for href, label, badge in items:
            on = "on" if label == active else ""
            b = '<span class="badge">%s</span>' % badge if badge else ""
            buf.append('<a class="%s" href="%s">%s%s</a>' % (on, href, label, b))
    buf.append("</nav>")
    return "\n".join(buf)


def shell(title, body, active, role="manager", crumb=None, navgroups=None, head=""):
    groups = navgroups if navgroups is not None else NAVS.get(role, NAV_MANAGER)
    crumb_html = crumb or "<b>%s</b>" % title
    side = ROLE_SIDE.get(role, "#1F2937")
    return """<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>%s · %s</title>
<link rel="stylesheet" href="../mock.css">
<style>%s</style></head>
<body><div class="shell">
<aside class="side" style="background:%s">
  <div class="brand"><span class="mark">云</span>云栖露营运营</div>
  %s
</aside>
<div class="main">
  <header class="top">
    <div class="crumb">%s</div><div class="spacer"></div>
    <button class="icon-btn" title="全局搜索">%s</button>
    <button class="icon-btn" title="消息">%s</button>
    <button class="icon-btn" title="帮助">%s</button>
    <div class="avatar">%s</div>
    <div style="font-size:13px">%s</div>
  </header>
  <main class="content">%s</main>
</div></div></body></html>""" % (title, APP, head, side, nav(groups, active), crumb_html,
                                  icon("search"), icon("bell"), icon("file"),
                                  ROLE_AVATAR.get(role, "顾"), ROLE_LABEL.get(role, "运营经理"), body)


def tag(text, cls="t-gray"):
    return '<span class="tag %s">%s</span>' % (cls, text)


def table(headers, rows):
    th = "".join("<th>%s</th>" % h for h in headers)
    trs = "".join("<tr>%s</tr>" % "".join("<td>%s</td>" % c for c in r) for r in rows)
    return '<table><thead><tr>%s</tr></thead><tbody>%s</tbody></table>' % (th, trs)


def pager(total=48):
    return ('<div class="pager"><span>共 %d 条</span>'
            '<select style="width:96px;height:26px"><option>10 条/页</option><option>20 条/页</option>'
            '<option>50 条/页</option></select>'
            '<span class="pg">上一页</span><span class="pg on">1</span><span class="pg">2</span>'
            '<span class="pg">3</span><span class="pg">4</span><span class="pg">5</span>'
            '<span class="pg">下一页</span></div>' % total)


def kpis(items):
    buf = ['<div class="kpis">']
    for label, value, delta, down in items:
        cls = "delta down" if down else "delta"
        buf.append('<div class="kpi"><div class="label">%s</div><div class="value">%s</div>'
                   '<div class="%s">%s</div></div>' % (label, value, cls, delta))
    buf.append("</div>")
    return "".join(buf)


def bars(values, color=None, h=110):
    mx = max(values) or 1
    buf = ['<div class="bars" style="height:%dpx">' % h]
    for i, v in enumerate(values):
        c = color[i] if color else "var(--secondary)"
        buf.append('<i style="height:%d%%;background:%s"></i>' % (max(6, int(v * 100 / mx)), c))
    buf.append("</div>")
    return "".join(buf)


def legend(items):
    return '<div class="legend">%s</div>' % "".join(
        '<span><i style="background:%s"></i>%s</span>' % (c, t) for c, t in items)


def card(title, inner, extra=""):
    return ('<div class="chart"><div class="ct">%s<span>%s</span></div>%s</div>' % (title, extra, inner))


def kv(pairs, cls=""):
    return '<div class="kv %s">%s</div>' % (cls, "".join(
        '<div><span>%s</span>%s</div>' % (k, v) for k, v in pairs))


PAGES = {}


def page(fn):
    PAGES[fn.__name__] = fn
    return fn


# ---------------- 01 登录 ----------------
@page
def p01():
    return """<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="utf-8"><title>登录 · 云栖露营地</title>
<link rel="stylesheet" href="../mock.css"><style>
.auth .art .route{display:flex;flex-direction:column;gap:10px;margin-top:28px;position:relative;z-index:2}
.auth .art .route div{display:flex;align-items:center;gap:10px;font-size:13px;color:#CFDCE4}
.auth .art .route b{width:20px;height:20px;border-radius:50%;border:1px solid #7FA2B6;display:grid;place-items:center;font-size:11px;font-weight:500;color:#DCE8EE}
.pitch-art{position:absolute;left:0;right:0;bottom:0;height:190px;opacity:.5}
.pitch-art span{position:absolute;border:1px solid rgba(255,255,255,.35);border-radius:4px}
</style></head>
<body><div class="auth">
  <div class="art">
    <div>
      <div style="display:flex;align-items:center;gap:10px;font-size:15px;font-weight:600">
        <span style="width:26px;height:26px;border-radius:7px;background:#9DB9CB;color:#1B2733;display:grid;place-items:center;font-size:13px">云</span>云栖露营地运营中心
      </div>
      <h1 style="margin-top:38px">营位、物资、<br>交接与结算，一处管完</h1>
      <p>从游客查询营位到退营验收结算，营地日常运营的八个环节都在同一条业务链上。物资库存与营位可用性同步校验，避免「营位能订、装备不够」。</p>
      <div class="badge-row"><span class="pill">营位日历</span><span class="pill">物资预占</span><span class="pill">交接留痕</span><span class="pill">线下结算</span></div>
      <div class="route">
        <div><b>1</b>查询营位 → 提交预约</div>
        <div><b>2</b>运营确认 → 物资备货</div>
        <div><b>3</b>到店入住 → 巡查服务</div>
        <div><b>4</b>退营验收 → 线下结算</div>
      </div>
    </div>
    <div style="font-size:12px;color:#8FA9B8;position:relative;z-index:2">© 2026 云栖露营地 · 仅登记线下收款结果，不接入在线支付</div>
    <div class="pitch-art">
      <span style="left:8%;bottom:26px;width:54px;height:34px"></span>
      <span style="left:19%;bottom:52px;width:54px;height:34px"></span>
      <span style="left:30%;bottom:22px;width:54px;height:34px"></span>
      <span style="left:41%;bottom:60px;width:54px;height:34px"></span>
      <span style="left:52%;bottom:30px;width:54px;height:34px"></span>
      <span style="left:63%;bottom:56px;width:54px;height:34px"></span>
      <span style="left:74%;bottom:24px;width:54px;height:34px"></span>
      <span style="left:85%;bottom:48px;width:54px;height:34px"></span>
    </div>
  </div>
  <div class="form"><div class="box">
    <h2>账号登录</h2>
    <div class="s">使用营地内部账号，或游客手机号快速查询预约</div>
    <div class="field"><label>账号 / 手机号</label><input value="admin" placeholder="请输入账号"></div>
    <div class="field"><label>密码</label><input type="password" value="********" placeholder="请输入密码"></div>
    <div class="field" style="display:flex;justify-content:space-between;align-items:center;font-size:13px;color:var(--text-sub)">
      <label style="display:flex;align-items:center;gap:6px;margin:0"><input type="checkbox" checked style="height:14px;width:14px">记住账号</label>
      <a href="#">忘记密码</a>
    </div>
    <button class="btn primary">登 录</button>
    <div class="hint" style="margin-top:14px;line-height:20px">连续 5 次密码错误将锁定 15 分钟。首次登录请及时修改初始密码。</div>
  </div></div>
</div></body></html>"""


# ---------------- 02 看板 ----------------
@page
def p02():
    body = """<h1>露营地经营看板</h1>
<div class="sub">数据截至 2026-11-01 14:00 · 口径：全部营区 · 对比上周</div>
%s
<div class="dash">
  <div class="chart wide"><div class="ct">近 12 日预订与入住趋势<span>单位：单</span></div>
    %s%s</div>
  <div class="chart"><div class="ct">营位状态分布<span>共 68 个营位</span></div>
    <div style="display:flex;align-items:center;gap:18px">
      <div class="donut"></div>
      <div style="font-size:12px;line-height:22px;color:var(--text-sub)">
        <div><b style="color:#1F2937">可预订 41</b></div><div>已预留 9</div><div>已入住 11</div>
        <div>待清洁 4</div><div>维修/停用 3</div>
      </div>
    </div></div>
  <div class="chart"><div class="ct">营位类型利用率<span>近 30 天</span></div>
    %s%s</div>
  <div class="chart"><div class="ct">物资租赁排行 TOP6<span>按出租件次</span></div>
    %s</div>
  <div class="chart"><div class="ct">收入构成<span>本月 ￥148,760</span></div>
    %s%s</div>
  <div class="chart"><div class="ct">取消原因分布<span>近 30 天 23 单</span></div>
    %s%s</div>
  <div class="chart"><div class="ct">联合可用性结果分布<span>创新模块 · 42 次计算</span></div>
    %s%s</div>
</div>""" % (
        kpis([("今日入住", "11<small>单</small>", "较昨日 +3", False),
              ("今日退营", "8<small>单</small>", "待验收 2", False),
              ("营位利用率", "72.4<small>&#37;</small>", "较上周 +5.1pt", False),
              ("待备货", "4<small>单</small>", "其中 1 单缺 2 项", True),
              ("待整改隐患", "3<small>项</small>", "最高级别：高", True),
              ("当月收入", "14.88<small>万</small>", "较上月 +12.6&#37;", False)]),
        bars([14, 19, 26, 31, 22, 27, 38, 41, 24, 28, 35, 44], ["#9DB9CB"] * 6 + ["#2F6B8A"] * 6, 126),
        legend([("#9DB9CB", "工作日"), ("#2F6B8A", "周末/节假日")]),
        bars([86, 74, 68, 52, 39], ["#1F2937", "#2F6B8A", "#3B627A", "#7C9AAE", "#9DB9CB"], 126),
        legend([("#1F2937", "草坪帐篷位"), ("#2F6B8A", "木平台位"), ("#3B627A", "房车位"), ("#7C9AAE", "团体包场"), ("#9DB9CB", "林间位")]),
        table(["物资", "件次"], [["四人帐篷", '<div class="progress" style="width:210px"><i style="width:100%"></i></div> 128'],
                              ["折叠桌椅组", '<div class="progress" style="width:210px"><i style="width:82%"></i></div> 105'],
                              ["卡式炉具", '<div class="progress" style="width:210px"><i style="width:64%"></i></div> 82']
                              ]).replace(
            "<table>", '<table style="font-size:12px">').replace("padding:10px 12px", "padding:7px 8px"),
        bars([62, 22, 9, 7], ["#1F2937", "#2F6B8A", "#9DB9CB", "#C6D3DC"], 126),
        legend([("#1F2937", "营位费 ￥92,340"), ("#2F6B8A", "物资租金 ￥38,120"), ("#9DB9CB", "超时与赔偿 ￥11,300"), ("#C6D3DC", "其他 ￥7,000")]),
        bars([9, 6, 4, 3, 1], ["#7C9AAE", "#2F6B8A", "#3B627A", "#9DB9CB", "#C6D3DC"], 126),
        legend([("#7C9AAE", "行程变更 9"), ("#2F6B8A", "天气 6"), ("#3B627A", "价格 4"), ("#9DB9CB", "重复下单 3"), ("#C6D3DC", "其他 1")]),
        bars([18, 14, 10], ["#2E7D5B", "#9A6B1F", "#B3261E"], 126),
        legend([("#2E7D5B", "推荐 18"), ("#9A6B1F", "有条件推荐 14"), ("#B3261E", "不推荐 10")]))
    head = """
.kpis{margin-bottom:10px}
.dash{display:grid;grid-template-columns:repeat(4,1fr);grid-template-rows:258px 258px;gap:14px}
.dash .chart{height:258px;box-sizing:border-box;overflow:hidden;padding:10px 12px}
.dash .chart.wide{grid-column:span 2}
.dash .chart .ct{margin-bottom:6px}
h1{font-size:20px;margin-bottom:0}
.sub{margin-bottom:8px}
.dash table td{padding:5px 6px!important}
"""
    return shell("经营看板", body, "经营看板", "manager", "<b>工作台</b> / 经营看板", head=head)


# ---------------- 03 游客门户 ----------------
@page
def p03():
    body = """<div class="pnav"><div class="logo">云栖露营地</div>
  <a class="on" href="#">营地首页</a><a href="04_site_map.html">营位查询</a><a href="#">服务设施</a>
  <a href="#">入住须知</a><a href="#">我的预约</a>
  <div style="flex:1"></div><a href="01_login.html" class="btn sm">员工登录</a></div>
<div class="pwrap">
  <div class="hero">
    <div style="position:relative;z-index:2">
      <h2>山谷里的 68 个营位，今晚还剩 12 个可订</h2>
      <p>草坪帐篷位、木平台位、房车位与林间位，均可在线查询实时日历。帐篷、桌椅、炉具等装备随预约一并预占，到店即用。</p>
      <div style="display:flex;gap:10px;margin-top:16px"><button class="btn">立即查询营位</button><button class="btn">查看营地地图</button></div>
    </div>
    <div class="hstat">
      <div><b>68</b><span>营位总数</span></div><div><b>72&#37;</b><span>本周利用率</span></div><div><b>4.7</b><span>综合评分</span></div>
    </div>
  </div>

  <div class="card" style="margin-top:16px">
    <div class="toolbar" style="margin-bottom:0">
      <select style="width:180px"><option>入住日期 2026-11-07</option></select>
      <select style="width:180px"><option>营位类型：全部</option></select>
      <select style="width:180px"><option>人数：2 人</option></select>
      <select style="width:180px"><option>是否可用电：不限</option></select>
      <button class="btn primary">查询</button><button class="btn">重置</button>
      <div style="flex:1"></div><span class="hint">高级筛选（面积、朝向、相邻环境）</span>
    </div>
  </div>

  <div class="row" style="margin-top:16px;align-items:flex-start">
    <div style="flex:1;min-width:0">
      <div class="row" style="gap:14px">
        <div class="card" style="flex:1;padding:0;overflow:hidden">
          <img class="thumb" style="height:150px;border-radius:0" src="../img/site-1.jpg" alt="草坪帐篷位">
          <div style="padding:12px 14px">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b style="font-size:14px">A 区草坪帐篷位 A-07</b>%s</div>
            <div class="hint" style="margin-top:4px">标准 2 人 / 最大 4 人 · 60㎡ · 可接电 · 近洗手间 40m</div>
            <div style="display:flex;justify-content:space-between;align-items:center;margin-top:10px">
              <span style="color:var(--primary);font-weight:600">￥280 <small style="font-weight:400;color:var(--text-sub)">/晚</small></span>
              <a class="btn sm" href="05_site_detail.html">查看详情</a></div>
          </div>
        </div>
        <div class="card" style="flex:1;padding:0;overflow:hidden">
          <img class="thumb" style="height:150px;border-radius:0" src="../img/site-2.jpg" alt="木平台位">
          <div style="padding:12px 14px">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b style="font-size:14px">B 区木平台位 B-12</b>%s</div>
            <div class="hint" style="margin-top:4px">标准 4 人 / 最大 6 人 · 90㎡ · 可接电 · 近水点 20m</div>
            <div style="display:flex;justify-content:space-between;align-items:center;margin-top:10px">
              <span style="color:var(--primary);font-weight:600">￥460 <small style="font-weight:400;color:var(--text-sub)">/晚</small></span>
              <a class="btn sm" href="05_site_detail.html">查看详情</a></div>
          </div>
        </div>
        <div class="card" style="flex:1;padding:0;overflow:hidden">
          <img class="thumb" style="height:150px;border-radius:0" src="../img/site-3.jpg" alt="房车位">
          <div style="padding:12px 14px">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b style="font-size:14px">C 区房车位 C-03</b>%s</div>
            <div class="hint" style="margin-top:4px">标准 3 人 / 最大 5 人 · 110㎡ · 独立水电桩</div>
            <div style="display:flex;justify-content:space-between;align-items:center;margin-top:10px">
              <span style="color:var(--primary);font-weight:600">￥520 <small style="font-weight:400;color:var(--text-sub)">/晚</small></span>
              <a class="btn sm" href="05_site_detail.html">查看详情</a></div>
          </div>
        </div>
      </div>

      <div class="card" style="margin-top:16px">
        <h2>营地介绍与服务设施</h2>
        <div class="hint" style="line-height:22px">云栖露营地占地 42 亩，分 A/B/C/D 四个营区，共 68 个营位。营地提供公共淋浴、洗衣、共享厨房、儿童沙池与夜间照明，24 小时值守。所有营位禁止明火，炊事须在指定区域使用炉具。</div>
        <div class="row" style="gap:10px;margin-top:12px">
          <span class="tag t-blue">公共淋浴</span><span class="tag t-blue">共享厨房</span><span class="tag t-blue">儿童沙池</span>
          <span class="tag t-blue">夜间照明</span><span class="tag t-blue">24h 值守</span><span class="tag t-blue">装备租赁</span>
        </div>
      </div>
    </div>

    <div style="width:330px;flex:0 0 330px">
      <div class="card"><h2>入住须知</h2>
        <ol class="hint" style="padding-left:18px;line-height:22px;margin:0">
          <li>入住 14:00 后，退营 12:00 前，超时按 50 元/小时计。</li>
          <li>禁止在营位内使用明火与炭炉，炊事请至共享厨房。</li>
          <li>22:00 后请降低音量，营地执行静音时段管理。</li>
          <li>宠物需全程牵引，禁止进入儿童活动区。</li>
          <li>恶劣天气预警时，营地有权要求转移至安全区域。</li>
        </ol>
      </div>
      <div class="card" style="margin-top:14px"><h2>我的预约</h2>
        <div style="font-size:13px;line-height:22px">
          <div style="display:flex;justify-content:space-between"><span class="hint">预约编号</span><b>YY20261105-0031</b></div>
          <div style="display:flex;justify-content:space-between"><span class="hint">营位</span><span>A-07 草坪帐篷位</span></div>
          <div style="display:flex;justify-content:space-between"><span class="hint">日期</span><span>11-07 至 11-09</span></div>
          <div style="display:flex;justify-content:space-between"><span class="hint">状态</span>%s</div>
        </div>
        <button class="btn" style="width:100%%;margin-top:12px">查看预约详情</button>
      </div>
    </div>
  </div>
</div>""" % (tag("可预订", "t-green"), tag("可预订", "t-green"), tag("已预留", "t-blue"), tag("已确认", "t-green"))
    head = ".pwrap{max-width:1180px}"
    return """<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8">
<title>营地首页 · 云栖露营地</title><link rel="stylesheet" href="../mock.css"><style>%s</style></head>
<body class="portal">%s</body></html>""" % (head, body)


# ---------------- 04 营位地图 ----------------
@page
def p04():
    pitches = ""
    data = [("A-01", "草坪帐篷位", "free"), ("A-02", "草坪帐篷位", "book"), ("A-03", "草坪帐篷位", "free"),
            ("A-04", "草坪帐篷位", "live"), ("A-05", "草坪帐篷位", "clean"), ("A-06", "草坪帐篷位", "free"),
            ("A-07", "草坪帐篷位", "free"), ("A-08", "草坪帐篷位", "book"), ("B-01", "木平台位", "free"),
            ("B-02", "木平台位", "free"), ("B-03", "木平台位", "fix"), ("B-04", "木平台位", "book"),
            ("B-05", "木平台位", "free"), ("B-06", "木平台位", "live"), ("B-07", "木平台位", "free"),
            ("B-08", "木平台位", "clean"), ("C-01", "房车位", "free"), ("C-02", "房车位", "book"),
            ("C-03", "房车位", "live"), ("C-04", "房车位", "free"), ("C-05", "房车位", "free"),
            ("C-06", "房车位", "fix"), ("D-01", "林间位", "free"), ("D-02", "林间位", "book")]
    for no, ty, st in data:
        pitches += '<div class="pitch %s"><div class="st"></div><div class="no">%s</div><div class="ty">%s</div></div>' % (st, no, ty)

    body = """<h1>营位地图与查询</h1>
<div class="sub">按日期与条件筛选营位，点击营位查看详情与可订日历</div>
<div class="card" style="padding:14px 18px;margin-bottom:14px">
  <div class="toolbar" style="margin-bottom:0">
    <input type="date" value="2026-11-07" style="width:170px">
    <input type="date" value="2026-11-09" style="width:170px">
    <select><option>营区：全部</option><option>A 区</option><option>B 区</option></select>
    <select><option>营位类型：全部</option><option>草坪帐篷位</option><option>木平台位</option></select>
    <select><option>人数：2 人</option><option>4 人</option><option>6 人</option></select>
    <select><option>可用状态：全部</option></select>
    <button class="btn primary">查询</button>
    <div style="flex:1"></div>
    <button class="btn">高级筛选</button>
  </div>
</div>
<div class="row" style="align-items:flex-start">
  <div class="card" style="flex:1;min-width:0">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
      <h2 style="margin:0">A 区 · 草坪帐篷区</h2>
      <div class="legend" style="margin:0">
        <span><i style="background:#2E7D5B"></i>可预订</span><span><i style="background:#2F6B8A"></i>已预留</span>
        <span><i style="background:#1F4A63"></i>已入住</span><span><i style="background:#9A6B1F"></i>待清洁</span>
        <span><i style="background:#B3261E"></i>维修/停用</span>
      </div>
    </div>
    <div class="map" style="margin-bottom:16px">%s</div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
      <h2 style="margin:0">B 区 · 木平台区</h2><span class="hint">共 8 个营位，可订 3 个</span></div>
    <div class="map" style="margin-bottom:16px">%s</div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
      <h2 style="margin:0">C 区 · 房车区</h2><span class="hint">共 6 个营位，可订 3 个</span></div>
    <div class="map">%s</div>
  </div>
  <div style="width:320px;flex:0 0 320px">
    <div class="card"><h2>当前筛选</h2>
      <div class="hint" style="line-height:22px">入住 2026-11-07 · 退营 2026-11-09 · 2 人 · 全部类型</div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:12px">
        <div class="kpi" style="padding:10px 12px"><div class="label">符合条件</div><div class="value" style="font-size:22px">24</div></div>
        <div class="kpi" style="padding:10px 12px"><div class="label">当前可订</div><div class="value" style="font-size:22px">14</div></div>
      </div>
      <div class="note" style="margin-top:12px">11-07 为周末，A/B 区基础价上浮 20&#37;。选择营位后可一并预占租赁物资，系统会同步校验库存。</div>
    </div>
    <div class="card" style="margin-top:14px"><h2>营区分布图</h2>
      <img class="thumb" style="height:150px" src="../img/camp-map.jpg" alt="营区地图">
      <div class="hint" style="margin-top:8px">A 区靠溪流、B 区临林地、C 区近出入口、D 区最安静</div>
    </div>
  </div>
</div>""" % (pitches[:len(pitches)] if False else "".join(
        '<div class="pitch %s"><div class="st"></div><div class="no">%s</div><div class="ty">%s</div></div>' % (s, n, t)
        for n, t, s in data[:8]),
    "".join('<div class="pitch %s"><div class="st"></div><div class="no">%s</div><div class="ty">%s</div></div>' % (s, n, t) for n, t, s in data[8:16]),
    "".join('<div class="pitch %s"><div class="st"></div><div class="no">%s</div><div class="ty">%s</div></div>' % (s, n, t) for n, t, s in data[16:22]))
    return shell("营位地图", body, "营区与营位", "manager", "<b>营地资源</b> / 营位地图")


# ---------------- 05 营位详情 ----------------
@page
def p05():
    cal_hd = "".join('<div class="hd">%s</div>' % d for d in ["一", "二", "三", "四", "五", "六", "日"])
    days = [(1, ""), (2, ""), (3, ""), (4, ""), (5, ""), (6, "ok"), (7, "warn"), (8, "ok"), (9, "ok"),
            (10, ""), (11, ""), (12, ""), (13, ""), (14, ""), (15, "ok"), (16, "ok"), (17, ""),
            (18, ""), (19, ""), (20, ""), (21, ""), (22, "ok"), (23, "ok"), (24, ""), (25, ""),
            (26, ""), (27, ""), (28, "ok"), (29, "ok"), (30, "")]
    cells = ""
    for d, ev in days:
        e = ""
        if d in (6, 7, 8, 9):
            e = '<div class="ev ok">已订</div>'
        elif d in (15, 16):
            e = '<div class="ev ok">已订</div>'
        elif d in (22, 23):
            e = '<div class="ev ok">已订</div>'
        elif d in (28, 29):
            e = '<div class="ev warn">预留</div>'
        cells += '<div class="cd"><b>%d</b>%s</div>' % (d, e)
    for _ in range(2):
        cells += '<div class="cd" style="background:#FAFBFC"></div>'

    body = """<h1>A-07 草坪帐篷位</h1>
<div class="sub">A 区 · 草坪帐篷位 · 编号 A-07 · 当前状态 %s</div>
<div class="row" style="align-items:flex-start">
  <div style="flex:1;min-width:0">
    <div class="card" style="padding:0;overflow:hidden">
      <div style="display:grid;grid-template-columns:1.5fr 1fr;gap:2px;background:var(--line)">
        <img src="../img/site-1.jpg" style="width:100%%;height:270px;object-fit:cover;display:block" alt="营位主图">
        <div style="display:grid;grid-template-rows:1fr 1fr;gap:2px">
          <img src="../img/site-4.jpg" style="width:100%%;height:134px;object-fit:cover;display:block" alt="营位环境">
          <img src="../img/site-5.jpg" style="width:100%%;height:134px;object-fit:cover;display:block" alt="营位设施">
        </div>
      </div>
    </div>
    <div class="card" style="margin-top:14px">
      <h2>营位信息</h2>
      %s
    </div>
    <div class="card" style="margin-top:14px">
      <h2>可订日历 · 2026 年 11 月</h2>
      <div class="cal">%s%s</div>
      <div class="legend" style="margin-top:10px"><span><i style="background:#2E7D5B"></i>可订</span>
        <span><i style="background:#B3261E"></i>已被预留</span><span><i style="background:#2F6B8A"></i>已入住</span></div>
    </div>
    <div class="card" style="margin-top:14px">
      <h2>相邻环境与配套</h2>
      <div class="row" style="gap:10px">
        <span class="tag t-blue">溪流 30m</span><span class="tag t-blue">洗手间 40m</span><span class="tag t-blue">取水点 25m</span>
        <span class="tag t-blue">树荫覆盖</span><span class="tag t-gray">不可明火</span><span class="tag t-gray">可接电 220V</span>
      </div>
      <div class="hint" style="margin-top:10px">营位地面为混播草坪，排水良好；相邻 A-06、A-08 之间有 6m 缓冲带，适合家庭出行。</div>
    </div>
  </div>
  <div style="width:340px;flex:0 0 340px">
    <div class="card"><h2>价格明细</h2>
      <div style="font-size:13px;line-height:26px">
        <div style="display:flex;justify-content:space-between"><span class="hint">工作日基础价</span><span>￥280 / 晚</span></div>
        <div style="display:flex;justify-content:space-between"><span class="hint">周末上浮（20&#37;）</span><span>￥336 / 晚</span></div>
        <div style="display:flex;justify-content:space-between"><span class="hint">节假日（11-21 起）</span><span>￥420 / 晚</span></div>
        <div style="display:flex;justify-content:space-between"><span class="hint">清洁费</span><span>￥30 / 次</span></div>
        <div style="border-top:1px solid var(--line);margin-top:8px;padding-top:8px;display:flex;justify-content:space-between">
          <span class="hint">2 晚预估</span><b style="color:var(--primary);font-size:16px">￥702</b></div>
      </div>
      <button class="btn primary" style="width:100%%;margin-top:14px">立即预约此营位</button>
      <button class="btn" style="width:100%%;margin-top:8px">加入比较</button>
      <div class="note" style="margin-top:12px">预约时可同时选择帐篷、桌椅、炉具等装备，系统按租用日期校验库存并给出替代组合。</div>
    </div>
    <div class="card" style="margin-top:14px"><h2>近期评价</h2>
      <div style="border-bottom:1px solid var(--line);padding-bottom:10px;margin-bottom:10px">
        <div style="display:flex;justify-content:space-between;font-size:13px"><b>陈女士</b><span class="hint">2026-10-19</span></div>
        <div style="font-size:12px;color:#9A6B1F;margin:3px 0">★★★★★ 4.8</div>
        <div class="hint" style="line-height:21px">树荫很足，孩子玩得很开心。晚上能听到溪水声，就是洗手间稍微远一点。</div>
      </div>
      <div>
        <div style="display:flex;justify-content:space-between;font-size:13px"><b>李先生</b><span class="hint">2026-10-05</span></div>
        <div style="font-size:12px;color:#9A6B1F;margin:3px 0">★★★★☆ 4.3</div>
        <div class="hint" style="line-height:21px">营位平整，接电方便。建议自备延长线。</div>
      </div>
    </div>
  </div>
</div>""" % (tag("可预订", "t-green"),
             kv([("营位编号", "A-07"), ("营位类型", "草坪帐篷位"), ("所属区域", "A 区 · 草坪帐篷区"),
                 ("标准容量", "2 人"), ("最大容量", "4 人"), ("占地面积", "60 ㎡"),
                 ("配套设施", "220V 接电、野餐桌、烧烤架位"), ("开放日期", "2026-03-01 至 2026-11-30"),
                 ("当前状态", '<span class="tag t-green">可预订</span>')]),
             cal_hd, cells)
    return shell("营位详情", body, "营区与营位", "manager", "<b>营地资源</b> / 营位地图 / <b>A-07</b>")


# ---------------- 06 营区与营位管理 ----------------
@page
def p06():
    rows = [
        ["A-01", "草坪帐篷位", "A 区", "2 / 4", "60 ㎡", "￥280", "220V、野餐桌", tag("可预订", "t-green"), "2026-10-31 18:20"],
        ["A-07", "草坪帐篷位", "A 区", "2 / 4", "60 ㎡", "￥280", "220V、野餐桌", tag("已入住", "t-blue"), "2026-11-01 09:12"],
        ["B-03", "木平台位", "B 区", "4 / 6", "90 ㎡", "￥460", "平台、遮阳棚", tag("维修中", "t-red"), "2026-10-28 15:40"],
        ["B-06", "木平台位", "B 区", "4 / 6", "90 ㎡", "￥460", "平台、遮阳棚", tag("已入住", "t-blue"), "2026-11-01 10:05"],
        ["C-02", "房车位", "C 区", "3 / 5", "110 ㎡", "￥520", "独立水电桩", tag("已预留", "t-amber"), "2026-10-30 11:30"],
        ["C-06", "房车位", "C 区", "3 / 5", "110 ㎡", "￥520", "独立水电桩", tag("停用", "t-gray"), "2026-10-26 08:15"],
        ["D-01", "林间位", "D 区", "2 / 3", "45 ㎡", "￥220", "林荫、吊床架", tag("待清洁", "t-amber"), "2026-11-01 11:48"],
        ["D-04", "林间位", "D 区", "2 / 3", "45 ㎡", "￥220", "林荫、吊床架", tag("可预订", "t-green"), "2026-10-29 16:22"],
    ]
    body = """<h1>营区与营位管理</h1>
<div class="sub">维护营区、营位编号、类型、容量、基础价格与状态；营位图片通过上传组件提交</div>
<div class="card">
  <div class="toolbar">
    <input class="search" placeholder="营位编号 / 名称">
    <select><option>所属区域：全部</option></select>
    <select><option>营位类型：全部</option></select>
    <select><option>营位状态：全部</option></select>
    <button class="btn primary">查询</button><button class="btn">重置</button>
    <div style="flex:1"></div>
    <button class="btn">导出</button><button class="btn ghost">批量停用</button><button class="btn primary">+ 新增营位</button>
  </div>
  %s
  %s
</div>
<div class="row" style="margin-top:16px">
  <div class="card" style="flex:1"><h2>营区概况</h2>%s</div>
  <div class="card" style="flex:1"><h2>状态说明</h2>
    <div class="hint" style="line-height:24px">可预订 → 游客可提交预约；已预留 → 已有确认预约但未到店；已入住 → 客人已办理入住；
    待清洁 → 退营后待保洁；维修中 → 设施维修暂停销售；停用 → 长期停售（需填写原因）。<br>
    状态变更由前台或运营经理操作，全部写入操作日志。</div>
  </div>
</div>""" % (table(["营位编号", "营位类型", "所属区域", "标准/最大容量", "面积", "基础价格", "配套设施", "状态", "更新时间", "操作"], rows),
             pager(68),
             kv([("营区数量", "4 个（A/B/C/D）"), ("营位总数", "68 个"), ("可预订", "41 个"),
                 ("维修停用", "3 个"), ("可用电营位", "52 个"), ("本周新增", "2 个")]))
    return shell("营区与营位", body, "营区与营位", "manager", "<b>营地资源</b> / 营区与营位")


# ---------------- 07 价格与停用 ----------------
@page
def p07():
    rows = [
        ["2026-11-07", "2026-11-09", "周末上浮", "A 区 / B 区", "×1.20", "生效", "顾青", "2026-10-20 10:12"],
        ["2026-11-21", "2026-11-23", "感恩节假期", "全部营区", "×1.50", "生效", "顾青", "2026-10-22 14:36"],
        ["2026-12-24", "2026-12-26", "圣诞专场", "A 区 / D 区", "×1.40", "待生效", "顾青", "2026-10-30 09:05"],
        ["2026-11-14", "2026-11-14", "企业团建包场", "C 区整体", "包场价 ￥6,800", "生效", "顾青", "2026-10-25 16:40"],
        ["2026-11-03", "2026-11-05", "B-03 维修停用", "B-03", "—", "生效", "唐磊", "2026-10-28 15:40"],
    ]
    body = """<h1>价格与停用管理</h1>
<div class="sub">特殊日期价格、包场占用与维修停用，全部按营位或营区维度生效</div>
<div class="card">
  <div class="toolbar">
    <select><option>类型：全部</option><option>特殊日期价格</option><option>包场占用</option><option>维修停用</option></select>
    <select><option>营区：全部</option></select>
    <input type="date" value="2026-11-01" style="width:160px">
    <button class="btn primary">查询</button>
    <div style="flex:1"></div>
    <button class="btn primary">+ 新增规则</button>
  </div>
  %s%s
</div>
<div class="row" style="margin-top:16px">
  <div class="card" style="flex:1"><h2>月度价格日历</h2>
    <div class="hint" style="margin-bottom:10px">点击日期可查看当日各营区实际售价</div>
    <div class="cal">%s%s</div>
  </div>
  <div class="card" style="width:360px"><h2>新增特殊价格</h2>
    <label class="f"><span>生效日期</span><input type="date" value="2026-11-07"></label>
    <label class="f"><span>结束日期</span><input type="date" value="2026-11-09"></label>
    <label class="f"><span>适用范围</span><select style="width:260px"><option>A 区 / B 区</option></select></label>
    <label class="f"><span>计价方式</span><select style="width:260px"><option>按倍率上浮</option><option>固定价</option></select></label>
    <label class="f"><span>倍率</span><input value="1.20" style="width:120px"></label>
    <label class="f" style="align-items:flex-start"><span>备注</span><textarea rows="2" style="width:260px">周末上浮 20&#37;</textarea></label>
    <div style="display:flex;gap:8px;justify-content:flex-end"><button class="btn">取消</button><button class="btn primary">保存</button></div>
  </div>
</div>""" % (table(["生效日期", "结束日期", "名称", "适用范围", "价格 / 处理", "状态", "操作人", "创建时间", "操作"], rows), pager(12),
             "".join('<div class="hd">%s</div>' % d for d in ["一", "二", "三", "四", "五", "六", "日"]),
             "".join('<div class="cd"><b>%d</b>%s</div>' % (d, '<div class="ev ok">￥280</div>' if d not in (7, 8, 21, 22) else '<div class="ev warn">￥420</div>')
                     for d in range(1, 31)))
    return shell("价格与停用", body, "价格与停用", "manager", "<b>营地资源</b> / 价格与停用")


# ---------------- 08 新建预约 ----------------
@page
def p08():
    body = """<h1>新建预约</h1>
<div class="sub">预订人：陈晓雯 · 手机 138****6721 · 系统将校验日期重叠、容量与停用时段</div>
<div class="row" style="align-items:flex-start">
  <div style="flex:1;min-width:0">
    <div class="card">
      <h2>营位与日期</h2>
      <div class="row" style="gap:24px">
        <div style="flex:1">
          <label class="f"><span>营位</span><select style="width:100%%"><option>A-07 草坪帐篷位（标准 2 / 最大 4 人）</option></select></label>
          <label class="f"><span>入住日期</span><input type="date" value="2026-11-07" style="width:100%%"></label>
          <label class="f"><span>退营日期</span><input type="date" value="2026-11-09" style="width:100%%"></label>
          <label class="f"><span>入住人数</span><input value="3" style="width:100%%"></label>
        </div>
        <div style="flex:1">
          <label class="f"><span>预订人</span><input value="陈晓雯" style="width:100%%"></label>
          <label class="f"><span>联系电话</span><input value="138 0013 6721" style="width:100%%"></label>
          <label class="f"><span>车辆数</span><input value="1" style="width:100%%"></label>
          <label class="f"><span>儿童/特殊人群</span><select style="width:100%%"><option>有儿童（需确认安全提示）</option><option>无</option></select></label>
        </div>
      </div>
      <div class="note">该营位在 11-07 至 11-09 期间无冲突预约，容量 3 人未超过最大容量 4 人。含 1 名 6 岁儿童，需勾选「儿童安全提示已确认」。</div>
      <label style="display:flex;align-items:center;gap:8px;margin-top:10px;font-size:13px"><input type="checkbox" checked style="width:14px;height:14px">已向客人确认儿童安全提示（远离溪流、夜间陪同）</label>
    </div>

    <div class="card" style="margin-top:14px">
      <h2>租赁物资需求</h2>
      <div class="toolbar">
        <select><option>物资分类：全部</option></select>
        <button class="btn sm">+ 添加物资</button><button class="btn sm">按人数推荐组合</button>
        <div style="flex:1"></div><span class="hint">物资可用性按 11-07 至 11-09 校验</span>
      </div>
      %s
      <div class="note" style="margin-top:10px">四人帐篷当前可用 6 顶（含周转缓冲已扣除），本次预占 1 顶；卡式炉具可用 9 台，预占 1 台。全部满足。</div>
    </div>

    <div class="card" style="margin-top:14px">
      <h2>同行人与车辆</h2>
      <div class="row" style="gap:20px">
        <div style="flex:1">
          <div class="hint" style="margin-bottom:8px">同行人（不含预订人）</div>
          %s
          <button class="btn sm">+ 添加同行人</button>
        </div>
        <div style="flex:1">
          <div class="hint" style="margin-bottom:8px">车辆登记</div>
          %s
          <button class="btn sm">+ 添加车辆</button>
        </div>
      </div>
    </div>
  </div>

  <div style="width:340px;flex:0 0 340px">
    <div class="card"><h2>费用预估</h2>
      <div style="font-size:13px;line-height:26px">
        <div style="display:flex;justify-content:space-between"><span class="hint">营位费（2 晚，周末价）</span><span>￥672</span></div>
        <div style="display:flex;justify-content:space-between"><span class="hint">清洁费</span><span>￥30</span></div>
        <div style="display:flex;justify-content:space-between"><span class="hint">物资租金</span><span>￥150</span></div>
        <div style="display:flex;justify-content:space-between"><span class="hint">押金（可退）</span><span>￥300</span></div>
        <div style="border-top:1px solid var(--line);margin-top:8px;padding-top:8px;display:flex;justify-content:space-between">
          <span class="hint">合计</span><b style="color:var(--primary);font-size:17px">￥852</b></div>
      </div>
      <div class="hint" style="margin-top:8px">押金在退营验收后按损坏情况退还，不计入收入。</div>
    </div>
    <div class="card" style="margin-top:14px"><h2>联合可用性预检</h2>
      <div style="display:flex;align-items:center;gap:10px;margin-bottom:10px">
        <span class="tag t-green" style="font-size:13px;padding:3px 10px">推荐</span>
        <b style="font-size:20px;color:var(--primary)">86.5</b><span class="hint">/ 100</span>
      </div>
      <div class="score-row"><span>营位时段</span><div class="bar"><i style="width:100%%"></i></div><span class="v">95</span></div>
      <div class="score-row"><span>容纳人数</span><div class="bar"><i style="width:88%%"></i></div><span class="v">88</span></div>
      <div class="score-row"><span>设施需求</span><div class="bar"><i style="width:80%%"></i></div><span class="v">80</span></div>
      <div class="score-row"><span>物资库存</span><div class="bar"><i style="width:92%%"></i></div><span class="v">92</span></div>
      <div class="score-row"><span>物资周转</span><div class="bar"><i style="width:70%%"></i></div><span class="v">70</span></div>
      <div class="score-row"><span>维护状态</span><div class="bar"><i style="width:100%%"></i></div><span class="v">100</span></div>
      <div class="hint" style="margin-top:6px">规则版本 v3 · 计算时间 2026-11-01 14:05:22</div>
      <a class="btn sm" href="15_combo.html" style="margin-top:10px;display:inline-block">查看完整计算依据</a>
    </div>
    <div style="display:flex;gap:8px;margin-top:14px">
      <button class="btn" style="flex:1">保存草稿</button><button class="btn primary" style="flex:2">提交预约</button>
    </div>
  </div>
</div>""" % (table(["物资", "规格", "单价", "需求数量", "可用数量", "小计", "操作"], [
        ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-1.jpg" style="width:36px;height:28px;object-fit:cover;border-radius:3px">四人帐篷</div>', "双门 / 铝杆", "￥60/晚", "1", '<span class="tag t-green">可用 6</span>', "￥120", '<a class="btn sm">删除</a>'],
        ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-3.jpg" style="width:36px;height:28px;object-fit:cover;border-radius:3px">折叠桌椅组</div>', "1 桌 4 椅", "￥40/晚", "1", '<span class="tag t-green">可用 12</span>', "￥80", '<a class="btn sm">删除</a>'],
        ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-5.jpg" style="width:36px;height:28px;object-fit:cover;border-radius:3px">卡式炉具</div>', "单头 / 含气罐", "￥25/晚", "1", '<span class="tag t-amber">可用 2</span>', "￥50", '<a class="btn sm">删除</a>'],
    ]).replace("￥120", "￥120").replace("￥80", "￥80").replace("￥50", "￥50"),
    table(["姓名", "联系方式", "年龄段", "紧急联系人", "操作"], [
        ["陈小雨", "138****6721", "儿童（6 岁）", "陈晓雯 138****6721", '<a class="btn sm">编辑</a>'],
        ["王琳", "139****2280", "成人", "王琳 139****2280", '<a class="btn sm">编辑</a>'],
    ]),
    table(["车牌", "车型", "预计到达", "操作"], [
        ["浙A·8F72K", "SUV", "11-07 15:30", '<a class="btn sm">编辑</a>'],
        ["浙A·2K19M", "轿车", "11-07 16:10", '<a class="btn sm">编辑</a>'],
    ]))
    return shell("新建预约", body, "预约管理", "reception", "<b>预约业务</b> / 预约管理 / <b>新建预约</b>")


# ---------------- 09 预约管理 ----------------
@page
def p09():
    rows = [
        ["YY20261101-0042", "陈晓雯", "A-07", "11-07 → 11-09", "3", "￥852", tag("待确认", "t-amber"), "林悦", "2026-11-01 13:42"],
        ["YY20261101-0039", "周立", "B-06", "11-01 → 11-03", "4", "￥1,120", tag("已入住", "t-blue"), "林悦", "2026-11-01 10:05"],
        ["YY20261031-0036", "许安然", "C-02", "11-08 → 11-10", "2", "￥1,340", tag("已确认", "t-green"), "林悦", "2026-10-31 16:28"],
        ["YY20261031-0031", "高远", "A-03", "10-31 → 11-02", "2", "￥590", tag("待备货", "t-amber"), "林悦", "2026-10-31 09:14"],
        ["YY20261030-0028", "冯雪", "D-01", "10-30 → 11-01", "2", "￥470", tag("待验收", "t-blue"), "林悦", "2026-10-30 18:52"],
        ["YY20261030-0025", "邓伟", "B-01", "11-15 → 11-17", "5", "￥1,580", tag("已取消", "t-gray"), "林悦", "2026-10-30 11:20"],
        ["YY20261029-0021", "邵敏", "A-05", "10-29 → 10-31", "2", "￥620", tag("已结算", "t-green"), "林悦", "2026-10-29 15:46"],
        ["YY20261028-0018", "蔡宇航", "C-04", "11-22 → 11-24", "3", "￥1,290", tag("已拒绝", "t-red"), "林悦", "2026-10-28 10:33"],
    ]
    body = """<h1>预约管理</h1>
<div class="sub">共 46 条预约 · 默认按「待本人处理优先、更新时间倒序」排列</div>
<div class="card">
  <div class="toolbar">
    <span class="tag t-blue">全部 46</span><span class="tag t-amber">待确认 6</span><span class="tag t-blue">待备货 4</span>
    <span class="tag t-blue">待验收 2</span><span class="tag t-green">已完成 28</span><span class="tag t-red">异常 3</span>
  </div>
  <div class="toolbar">
    <input class="search" placeholder="预约编号 / 预订人 / 手机号">
    <select><option>营位：全部</option></select>
    <select><option>预约状态：全部</option></select>
    <input type="date" value="2026-11-01" style="width:160px">
    <button class="btn primary">查询</button><button class="btn">重置</button>
    <div style="flex:1"></div>
    <button class="btn">导出当前筛选</button><button class="btn ghost">批量确认</button>
  </div>
  %s%s
</div>
<div class="row" style="margin-top:16px">
  <div class="card" style="flex:1"><h2>状态流转</h2>
    <div class="flow"><span class="st done">待确认</span><span class="arw">→</span><span class="st done">已确认</span>
      <span class="arw">→</span><span class="st on">待备货</span><span class="arw">→</span><span class="st">已入住</span>
      <span class="arw">→</span><span class="st">待验收</span><span class="arw">→</span><span class="st">已结算</span></div>
    <div class="hint" style="margin-top:10px">取消、拒绝、爽约、改期均需填写原因（不少于 5 个字），变更记录保留在预约变更表中。</div>
  </div>
  <div class="card" style="flex:1"><h2>待处理提醒</h2>
    <div class="hint" style="line-height:24px">YY20261101-0042 待确认已 4 小时，超过 2 小时响应阈值；<br>
    YY20261031-0031 待备货，需仓库今日 17:00 前完成出库；<br>
    YY20261030-0028 退营未验收，客人已到前台。</div>
  </div>
</div>""" % (table(["预约编号", "预订人", "营位", "入住 → 退营", "人数", "预估金额", "状态", "负责人", "更新时间", "操作"], rows), pager(46))
    return shell("预约管理", body, "预约管理", "reception", "<b>预约业务</b> / 预约管理")


# ---------------- 10 预约日历 ----------------
@page
def p10():
    hd = "".join('<div class="hd">%s</div>' % d for d in ["一", "二", "三", "四", "五", "六", "日"])
    ev = {1: [("A-01 入住", "ok")], 2: [("B-06 退营", "")], 3: [("C-02 入住", "ok")], 4: [],
          5: [("A-07 入住", "ok")], 6: [("D-01 退营", "")], 7: [("A-03 入住", "ok"), ("维护 B-03", "warn")],
          8: [("C-04 入住", "ok")], 9: [], 10: [("B-01 入住", "ok")], 11: [("A-05 退营", "")],
          12: [], 13: [], 14: [("包场 C 区", "warn")], 15: [("A-02 入住", "ok")], 16: [], 17: [],
          18: [], 19: [], 20: [("B-05 入住", "ok")], 21: [("会议占用", "warn")], 22: [], 23: [],
          24: [], 25: [("A-08 入住", "ok")], 26: [], 27: [], 28: [("C-06 停用", "warn")], 29: [], 30: []}
    cells = ""
    for d in range(1, 31):
        inner = "".join('<div class="ev %s">%s</div>' % (c, t) for t, c in ev.get(d, []))
        cells += '<div class="cd"><b>%d</b>%s</div>' % (d, inner)
    cells += '<div class="cd" style="background:#FAFBFC"></div>' * 2

    body = """<h1>预约日历</h1>
<div class="sub">2026 年 11 月 · 按营位与日期查看占用，虚线为维护与包场占用</div>
<div class="card">
  <div class="toolbar">
    <button class="btn sm">‹ 上月</button><b style="font-size:14px;margin:0 6px">2026 年 11 月</b><button class="btn sm">下月 ›</button>
    <select><option>视图：月度日历</option><option>视图：营位甘特</option></select>
    <select><option>营区：全部</option></select>
    <div style="flex:1"></div>
    <button class="btn primary">+ 新建预约</button>
  </div>
  <div class="cal">%s%s</div>
  <div class="legend" style="margin-top:10px"><span><i style="background:#2E7D5B"></i>入住</span>
    <span><i style="background:#E9EFF3"></i>退营</span><span><i style="background:#B3261E"></i>维护 / 包场</span></div>
</div>
<div class="row" style="margin-top:16px">
  <div class="card" style="flex:1"><h2>今日到店与离店</h2>
    %s
  </div>
  <div class="card" style="width:360px"><h2>未来 7 天可订余量</h2>
    %s
  </div>
</div>""" % (hd, cells,
             table(["类型", "预约编号", "营位", "客人", "人数", "时间", "状态"], [
                 ["到店", "YY20261101-0039", "B-06", "周立", "4", "14:20", tag("已入住", "t-blue")],
                 ["到店", "YY20261101-0042", "A-07", "陈晓雯", "3", "15:30", tag("待确认", "t-amber")],
                 ["离店", "YY20261030-0028", "D-01", "冯雪", "2", "11:10", tag("待验收", "t-blue")],
                 ["到店", "YY20261031-0036", "C-02", "许安然", "2", "16:00", tag("已确认", "t-green")],
             ]),
             table(["日期", "可订", "已订", "利用率"], [
                 ["11-02 周一", "48", "20", "29.4%"], ["11-03 周二", "52", "16", "23.5%"],
                 ["11-04 周三", "50", "18", "26.4%"], ["11-05 周四", "44", "24", "35.2%"],
                 ["11-06 周五", "26", "42", "61.7%"], ["11-07 周六", "14", "54", "79.4%"],
                 ["11-08 周日", "18", "50", "73.5%"],
             ]))
    return shell("预约日历", body, "预约日历", "reception", "<b>预约业务</b> / 预约日历")


# ---------------- 11 入住核验 ----------------
@page
def p11():
    body = """<h1>入住核验与安全交接 · YY20261101-0039</h1>
<div class="sub">营位 B-06 · 周立 · 4 人 · 11-01 至 11-03 · 当前状态 %s</div>
<div class="card" style="margin-bottom:14px">
  <div class="flow"><span class="st done">待确认</span><span class="arw">→</span><span class="st done">已确认</span>
    <span class="arw">→</span><span class="st done">待备货</span><span class="arw">→</span><span class="st on">已入住</span>
    <span class="arw">→</span><span class="st">待验收</span><span class="arw">→</span><span class="st">已结算</span></div>
</div>
<div class="row" style="align-items:flex-start">
  <div style="flex:1;min-width:0">
    <div class="card">
      <h2>核验信息</h2>
      <div class="row" style="gap:24px">
        <div style="flex:1">
          <label class="f"><span>实际入住人数</span><input value="4" style="width:100%%"></label>
          <label class="f"><span>实际营位</span><select style="width:100%%"><option>B-06 木平台位</option></select></label>
          <label class="f"><span>押金金额</span><input value="300.00" style="width:100%%"></label>
          <label class="f"><span>押金方式</span><select style="width:100%%"><option>微信</option><option>现金</option><option>支付宝</option></select></label>
        </div>
        <div style="flex:1">
          <label class="f"><span>登记人</span><input value="林悦" style="width:100%%" disabled></label>
          <label class="f"><span>到店时间</span><input value="2026-11-01 14:20:00" style="width:100%%"></label>
          <label class="f"><span>安全须知</span><select style="width:100%%"><option>已逐条告知并确认</option></select></label>
          <label class="f"><span>随行宠物</span><select style="width:100%%"><option>无</option><option>有（已牵引）</option></select></label>
        </div>
      </div>
      <div class="note" style="margin-top:6px">房车营位需确认水电桩接通正常、灭火器在位。本次 B-06 已由巡检记录确认设施正常（2026-10-31 17:20）。</div>
    </div>

    <div class="card" style="margin-top:14px">
      <h2>营位与物资交接单</h2>
      <div class="toolbar"><span class="hint">交接单号 JJ20261101-0018 · 客人确认后不可修改</span>
        <div style="flex:1"></div><button class="btn sm">打印交接单</button><button class="btn sm">客人签字确认</button></div>
      %s
    </div>

    <div class="card" style="margin-top:14px">
      <h2>现场交接照片</h2>
      <div class="hint" style="margin-bottom:10px">上传营位与物资现状照片，用于退营时责任界定；单张不超过 5MB，仅支持 jpg/png</div>
      <div class="row" style="gap:12px">
        <div><img class="thumb" style="width:150px;height:104px" src="../img/handover-1.jpg" alt="营位现状"><div class="hint" style="margin-top:4px">营位地面</div></div>
        <div><img class="thumb" style="width:150px;height:104px" src="../img/handover-2.jpg" alt="帐篷"><div class="hint" style="margin-top:4px">帐篷外观</div></div>
        <div><img class="thumb" style="width:150px;height:104px" src="../img/handover-3.jpg" alt="桌椅"><div class="hint" style="margin-top:4px">桌椅成套</div></div>
        <div style="width:150px;height:104px;border:1px dashed var(--line);border-radius:6px;display:grid;place-items:center;color:var(--text-sub);font-size:12px">+ 上传照片</div>
      </div>
    </div>
  </div>

  <div style="width:340px;flex:0 0 340px">
    <div class="card"><h2>预约摘要</h2>
      %s
      <div class="note" style="margin-top:10px">客人已确认安全须知：禁用明火、22:00 静音、儿童需成人陪同。</div>
    </div>
    <div class="card" style="margin-top:14px"><h2>物资领用确认</h2>
      <div class="hint" style="line-height:22px">四人帐篷 ×1（序列号 ZP-2024-0087）<br>折叠桌椅组 ×1（ZH-2023-0152）<br>卡式炉具 ×1（LJ-2025-0033，含气罐 1 只）</div>
      <label style="display:flex;align-items:center;gap:8px;margin-top:10px;font-size:13px"><input type="checkbox" checked style="width:14px;height:14px">客人已当面清点并确认领取</label>
    </div>
    <div style="display:flex;gap:8px;margin-top:14px">
      <button class="btn" style="flex:1">退回</button><button class="btn primary" style="flex:2">确认入住</button>
    </div>
  </div>
</div>""" % (tag("已入住", "t-blue"),
             table(["物资", "规格 / 序列号", "应收", "实发", "状态", "备注"], [
                 ["四人帐篷", "ZP-2024-0087", "1", "1", tag("完好", "t-green"), "含地钉、风绳"],
                 ["折叠桌椅组", "ZH-2023-0152", "1", "1", tag("完好", "t-green"), "1 桌 4 椅"],
                 ["卡式炉具", "LJ-2025-0033", "1", "1", tag("完好", "t-green"), "含气罐 1 只"],
                 ["营地灯", "DG-2025-0112", "2", "2", tag("完好", "t-green"), "含电池"],
             ]),
             kv([("预约编号", "YY20261101-0039"), ("客人", "周立 138****4201"), ("营位", "B-06 木平台位"),
                 ("入住", "2026-11-01 14:00"), ("退营", "2026-11-03 12:00"), ("人数", "4 人 / 1 儿童"),
                 ("金额", "￥1,120 + 押金 ￥300"), ("状态", tag("已入住", "t-blue")), ("负责人", "林悦")]))
    return shell("入住核验", body, "入住核验", "reception", "<b>预约业务</b> / 入住核验 / <b>YY20261101-0039</b>")


# ---------------- 12 退营结算 ----------------
@page
def p12():
    body = """<h1>退营验收与结算 · YY20261030-0028</h1>
<div class="sub">营位 D-01 · 冯雪 · 2 人 · 10-30 至 11-01 · 需完成物资归还与营位验收后方可结算</div>
<div class="row" style="align-items:flex-start">
  <div style="flex:1;min-width:0">
    <div class="card">
      <h2>退营验收</h2>
      <div class="row" style="gap:24px">
        <div style="flex:1">
          <label class="f"><span>营位卫生</span><select style="width:100%%"><option>合格（无需清洁费）</option><option>需深度清洁 +￥80</option></select></label>
          <label class="f"><span>物资数量</span><select style="width:100%%"><option>齐全</option><option>缺失</option></select></label>
          <label class="f"><span>损坏情况</span><select style="width:100%%"><option>轻微破损（赔偿 ￥60）</option><option>无损坏</option><option>严重损坏</option></select></label>
        </div>
        <div style="flex:1">
          <label class="f"><span>实际退营时间</span><input value="2026-11-01 14:40:00" style="width:100%%"></label>
          <label class="f"><span>超时时长</span><input value="2.7 小时（应退 12:00）" style="width:100%%" disabled></label>
          <label class="f"><span>超时费</span><input value="135.00" style="width:100%%" disabled></label>
        </div>
      </div>
      <div class="row" style="gap:12px;margin-top:4px">
        <div><img class="thumb" style="width:150px;height:104px" src="../img/checkout-1.jpg" alt="营位现状"><div class="hint" style="margin-top:4px">营位地面</div></div>
        <div><img class="thumb" style="width:150px;height:104px" src="../img/checkout-2.jpg" alt="损坏凭证"><div class="hint" style="margin-top:4px">帐篷支杆变形</div></div>
        <div><img class="thumb" style="width:150px;height:104px" src="../img/checkout-3.jpg" alt="归还物资"><div class="hint" style="margin-top:4px">归还清点</div></div>
        <div style="width:150px;height:104px;border:1px dashed var(--line);border-radius:6px;display:grid;place-items:center;color:var(--text-sub);font-size:12px">+ 上传凭证</div>
      </div>
    </div>

    <div class="card" style="margin-top:14px">
      <h2>物资归还明细</h2>
      %s
    </div>

    <div class="card" style="margin-top:14px">
      <h2>费用汇总</h2>
      %s
      <div class="note" style="margin-top:10px">费用明细由后端根据营位费、物资租金、超时费、损坏赔偿、押金退还与优惠自动汇总，人工调整需填写原因并留痕。</div>
    </div>
  </div>

  <div style="width:360px;flex:0 0 360px">
    <div class="card"><h2>结算单</h2>
      <div style="font-size:13px;line-height:24px">
        <div style="display:flex;justify-content:space-between"><span class="hint">应收合计</span><b>￥1,205</b></div>
        <div style="display:flex;justify-content:space-between"><span class="hint">押金</span><span>￥300</span></div>
        <div style="display:flex;justify-content:space-between"><span class="hint">押金退还</span><span style="color:var(--success)">-￥240</span></div>
        <div style="display:flex;justify-content:space-between"><span class="hint">押金抵扣</span><span style="color:var(--danger)">-￥60</span></div>
        <div style="border-top:1px solid var(--line);margin-top:8px;padding-top:8px;display:flex;justify-content:space-between">
          <span class="hint">客人应付</span><b style="font-size:17px;color:var(--primary)">￥965</b></div>
      </div>
      <label class="f" style="margin-top:12px;align-items:flex-start"><span>收款方式</span>
        <select style="width:220px"><option>微信</option><option>现金</option><option>支付宝</option></select></label>
      <label class="f" style="align-items:flex-start"><span>人工调整原因</span><textarea rows="2" style="width:220px">帐篷支杆变形，按采购价六折赔偿</textarea></label>
      <div class="hint">仅登记线下收款结果，不接入支付接口。</div>
      <button class="btn primary" style="width:100%%;margin-top:12px">确认收款并结算</button>
      <button class="btn" style="width:100%%;margin-top:8px">暂存结算单</button>
    </div>
    <div class="card" style="margin-top:14px"><h2>操作留痕</h2>
      <ul class="timeline" style="margin-top:10px">
        <li><b>提交退营验收</b><div class="m">林悦 · 2026-11-01 14:41</div><div class="hint">物资 3 项归还齐全，帐篷支杆变形</div></li>
        <li><b>仓库确认归还</b><div class="m">唐磊 · 2026-11-01 14:52</div><div class="hint">帐篷转维修，其余入库</div></li>
        <li><b>生成费用明细</b><div class="m">系统 · 2026-11-01 14:52</div><div class="hint">规则：超时 50 元/小时，损坏按六折</div></li>
      </ul>
    </div>
  </div>
</div>""" % (table(["物资", "应收", "实收", "归还状态", "损坏", "赔偿", "后续处理"], [
        ["四人帐篷", "1", "1", tag("已归还", "t-green"), tag("支杆变形", "t-amber"), "￥60", "转维修"],
        ["折叠桌椅组", "1", "1", tag("已归还", "t-green"), tag("完好", "t-green"), "￥0", "入库"],
        ["营地灯", "2", "2", tag("已归还", "t-green"), tag("完好", "t-green"), "￥0", "入库"],
    ]),
    table(["费用项", "计算说明", "金额"], [
        ["营位费", "D-01 ￥220 × 2 晚（工作日）", "￥440"],
        ["物资租金", "帐篷 ￥60×2 + 桌椅 ￥40×2 + 营地灯 ￥10×2×2", "￥240"],
        ["超时费", "超时 2.7 小时 × ￥50/小时", "￥135"],
        ["损坏赔偿", "帐篷支杆变形，采购价 ￥100 × 60%", "￥60"],
        ["清洁费", "卫生合格，免收", "￥0"],
        ["优惠", "会员 9 折券", "-￥30"],
        ["押金退还", "押金 ￥300 − 赔偿 ￥60", "￥240"],
    ]))
    return shell("退营结算", body, "退营结算", "reception", "<b>预约业务</b> / 退营结算 / <b>YY20261030-0028</b>")


# ---------------- 13 物资档案 ----------------
@page
def p13():
    rows = [
        ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-1.jpg" style="width:40px;height:30px;object-fit:cover;border-radius:3px">四人帐篷</div>', "帐篷", "双门 / 铝杆 / 240×210cm", "18", "6", "￥800", "￥60/晚", tag("可用", "t-green"), "唐磊", "2026-10-30 09:20"],
        ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-2.jpg" style="width:40px;height:30px;object-fit:cover;border-radius:3px">六人隧道帐</div>', "帐篷", "双卧 / 420×300cm", "8", "3", "￥1,600", "￥120/晚", tag("可用", "t-green"), "唐磊", "2026-10-30 09:22"],
        ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-3.jpg" style="width:40px;height:30px;object-fit:cover;border-radius:3px">折叠桌椅组</div>', "桌椅", "1 桌 4 椅 / 铝合金", "24", "12", "￥260", "￥40/晚", tag("可用", "t-green"), "唐磊", "2026-10-29 16:05"],
        ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-4.jpg" style="width:40px;height:30px;object-fit:cover;border-radius:3px">天幕 3×3</div>', "天幕", "涂银 / 含杆", "14", "4", "￥420", "￥35/晚", tag("可用", "t-green"), "唐磊", "2026-10-29 16:08"],
        ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-5.jpg" style="width:40px;height:30px;object-fit:cover;border-radius:3px">卡式炉具</div>', "炊具", "单头 / 含气罐", "16", "2", "￥180", "￥25/晚", tag("紧张", "t-amber"), "唐磊", "2026-11-01 08:40"],
        ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-6.jpg" style="width:40px;height:30px;object-fit:cover;border-radius:3px">营地灯</div>', "照明", "充电式 / 300lm", "30", "9", "￥120", "￥10/晚", tag("可用", "t-green"), "唐磊", "2026-10-28 14:12"],
        ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-7.jpg" style="width:40px;height:30px;object-fit:cover;border-radius:3px">睡袋（单人）</div>', "寝具", "可拼接 / 5℃", "20", "0", "￥150", "￥15/晚", tag("无库存", "t-red"), "唐磊", "2026-11-01 09:10"],
        ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-8.jpg" style="width:40px;height:30px;object-fit:cover;border-radius:3px">蛋卷桌</div>', "桌椅", "120×60cm", "12", "5", "￥210", "￥20/晚", tag("可用", "t-green"), "唐磊", "2026-10-27 11:33"],
    ]
    body = """<h1>物资档案与库存</h1>
<div class="sub">帐篷、天幕、桌椅、照明、炊具等租赁物资；库存含在库、在租与维修三类</div>
<div class="card">
  <div class="toolbar">
    <input class="search" placeholder="物资名称 / 编号">
    <select><option>物资分类：全部</option><option>帐篷</option><option>桌椅</option></select>
    <select><option>库存状态：全部</option><option>充足</option><option>紧张</option><option>无库存</option></select>
    <button class="btn primary">查询</button><button class="btn">重置</button>
    <div style="flex:1"></div>
    <button class="btn">导出</button><button class="btn primary">+ 新增物资</button>
  </div>
  %s%s
</div>
<div class="row" style="margin-top:16px">
  <div class="card" style="flex:1"><h2>库存结构</h2>%s</div>
  <div class="card" style="width:380px"><h2>库存变动（近 7 天）</h2>
    %s
    <div class="legend"><span><i style="background:#2E7D5B"></i>入库</span><span><i style="background:#2F6B8A"></i>出库</span></div>
  </div>
</div>""" % (table(["物资", "分类", "规格", "总数", "在租", "押金", "租金", "状态", "负责人", "更新时间", "操作"], rows), pager(26),
             kv([("物资种类", "26 种"), ("在库总数", "142 件"), ("在租", "41 件"), ("维修中", "5 件"),
                 ("平均出租率", "63.2%"), ("本月租金收入", "￥38,120")]),
             table(["日期", "入库", "出库"], [["10-26", "6", "12"], ["10-27", "0", "9"], ["10-28", "4", "14"],
                                          ["10-29", "0", "18"], ["10-30", "2", "11"], ["10-31", "8", "26"], ["11-01", "0", "7"]]))
    return shell("物资档案", body, "物资档案", "warehouse", "<b>物资租赁</b> / 物资档案")


# ---------------- 14 备货与发放 ----------------
@page
def p14():
    body = """<h1>备货与发放</h1>
<div class="sub">按今日入住生成备货清单，记录发放数量、序列号与物品状态；回收后转待检</div>
<div class="card" style="margin-bottom:14px">
  <div class="toolbar" style="margin-bottom:0">
    <input type="date" value="2026-11-01" style="width:160px">
    <select><option>状态：待备货</option><option>已备货</option><option>已发放</option><option>已归还</option></select>
    <button class="btn primary">生成备货清单</button>
    <div style="flex:1"></div>
    <span class="hint">今日待备货 4 单 · 涉及物资 11 项</span>
  </div>
</div>

<div class="row" style="align-items:flex-start">
  <div style="flex:1;min-width:0">
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
        <h2 style="margin:0">备货单 BH20261101-0007</h2>
        <div><span class="tag t-amber">待备货</span> <span class="hint">预约 YY20261101-0039 · 营位 B-06 · 周立</span></div>
      </div>
      %s
      <div class="toolbar" style="margin-top:12px">
        <label style="display:flex;align-items:center;gap:6px;font-size:13px"><input type="checkbox" checked style="width:14px;height:14px">全部物资已备齐并拍照</label>
        <div style="flex:1"></div>
        <button class="btn">打印备货单</button><button class="btn primary">确认出库</button>
      </div>
    </div>

    <div class="card" style="margin-top:14px">
      <h2>今日发放记录</h2>
      %s
      %s
    </div>
  </div>

  <div style="width:340px;flex:0 0 340px">
    <div class="card"><h2>库存预警</h2>
      <div style="font-size:13px;line-height:22px">
        <div style="display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid var(--line)">
          <span>睡袋（单人）</span><span class="tag t-red">已租完</span></div>
        <div style="display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid var(--line)">
          <span>卡式炉具</span><span class="tag t-amber">仅剩 2</span></div>
        <div style="display:flex;justify-content:space-between;padding:6px 0">
          <span>六人隧道帐</span><span class="tag t-amber">仅剩 3</span></div>
      </div>
      <div class="note" style="margin-top:10px">11-07 周末预订已达 54 单，建议提前补充睡袋 10 条、炉具 6 台。</div>
    </div>
    <div class="card" style="margin-top:14px"><h2>回收待检</h2>
      <div class="hint" style="line-height:22px">帐篷 ZP-2024-0087（支杆变形，转维修）<br>桌椅 ZH-2023-0152（完好）<br>营地灯 DG-2025-0112 ×2（完好）</div>
      <button class="btn" style="width:100%%;margin-top:10px">批量入库</button>
    </div>
  </div>
</div>""" % (table(["物资", "需求", "库存可用", "分配序列号", "状态", "操作"], [
        ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-1.jpg" style="width:36px;height:28px;object-fit:cover;border-radius:3px">四人帐篷</div>', "1", "6", "ZP-2024-0087", tag("已分配", "t-green"), '<a class="btn sm">更换</a>'],
        ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-3.jpg" style="width:36px;height:28px;object-fit:cover;border-radius:3px">折叠桌椅组</div>', "1", "12", "ZH-2023-0152", tag("已分配", "t-green"), '<a class="btn sm">更换</a>'],
        ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-5.jpg" style="width:36px;height:28px;object-fit:cover;border-radius:3px">卡式炉具</div>', "1", "2", "LJ-2025-0033", tag("已分配", "t-green"), '<a class="btn sm">更换</a>'],
        ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-6.jpg" style="width:36px;height:28px;object-fit:cover;border-radius:3px">营地灯</div>', "2", "9", "DG-2025-0112 / 0113", tag("已分配", "t-green"), '<a class="btn sm">更换</a>'],
    ]),
    table(["时间", "备货单", "预约", "营位", "物资件数", "发放人", "状态"], [
        ["11-01 09:20", "BH20261101-0005", "YY20261031-0031", "A-03", "5", "唐磊", tag("已发放", "t-green")],
        ["11-01 08:55", "BH20261101-0004", "YY20261031-0033", "C-01", "3", "唐磊", tag("已发放", "t-green")],
        ["11-01 08:30", "BH20261101-0003", "YY20261030-0028", "D-01", "4", "唐磊", tag("已归还", "t-blue")],
        ["10-31 17:10", "BH20261031-0021", "YY20261031-0031", "A-03", "5", "唐磊", tag("已发放", "t-green")],
    ]), pager(24))
    return shell("备货与发放", body, "备货与发放", "warehouse", "<b>物资租赁</b> / 备货与发放")


# ---------------- 15 联合可用性（创新点） ----------------
@page
def p15():
    body = """<h1>营位与租赁物资联合可用性</h1>
<div class="sub">在推荐营位时同步检查物资库存与周转缓冲，避免「营位可订、装备不足」；结果含缺口与替代组合</div>
<div class="row" style="align-items:flex-start">
  <div style="flex:1;min-width:0">
    <div class="card">
      <h2>计算输入</h2>
      <div class="row" style="gap:24px">
        <div style="flex:1">
          <label class="f"><span>营位</span><select style="width:100%%"><option>A-07 草坪帐篷位</option><option>B-06 木平台位</option></select></label>
          <label class="f"><span>入住日期</span><input type="date" value="2026-11-07" style="width:100%%"></label>
          <label class="f"><span>退营日期</span><input type="date" value="2026-11-09" style="width:100%%"></label>
        </div>
        <div style="flex:1">
          <label class="f"><span>人数</span><input value="3" style="width:100%%"></label>
          <label class="f"><span>设施需求</span><select style="width:100%%"><option>接电 + 野餐桌</option></select></label>
          <label class="f"><span>物资组合</span><select style="width:100%%"><option>四人帐篷 + 桌椅 + 炉具</option></select></label>
        </div>
      </div>
      <div class="toolbar" style="margin-bottom:0"><button class="btn primary">重新计算</button>
        <button class="btn">保存为草稿</button><div style="flex:1"></div>
        <span class="hint">规则版本 v3（生效 2026-10-15）· 权重合计 100&#37;</span></div>
    </div>

    <div class="row" style="margin-top:14px;gap:14px">
      <div class="card" style="flex:1.35">
        <h2>计算结果与各项得分</h2>
        <div style="display:flex;align-items:center;gap:14px;margin-bottom:14px">
          <div style="font-size:34px;font-weight:600;color:var(--primary);font-variant-numeric:tabular-nums">86.5</div>
          <div><span class="tag t-green" style="font-size:13px;padding:3px 10px">推荐</span>
            <div class="hint" style="margin-top:4px">总分 ≥ 80 判为推荐，60–79 为有条件推荐，&lt; 60 为不推荐</div></div>
        </div>
        %s
      </div>
      <div class="card" style="width:320px">
        <h2>主要影响因素</h2>
        <div style="font-size:13px;line-height:22px">
          <div style="margin-bottom:8px"><b style="color:var(--success)">正向：</b>营位时段完全空闲（11-07 至 11-09 无冲突），维护状态正常。</div>
          <div style="margin-bottom:8px"><b style="color:var(--danger)">扣分：</b>物资周转时间偏紧，11-07 当日帐篷归还集中在 12:00，备货窗口仅 2 小时。</div>
          <div><b style="color:var(--warning)">提示：</b>3 人入住 4 人帐，容纳人数得分 88，仍有余量。</div>
        </div>
        <h2 style="margin-top:14px">未满足条件</h2>
        <div class="hint" style="line-height:22px">无硬性不满足项。建议增配 1 顶天幕以应对降雨预警。</div>
      </div>
    </div>

    <div class="card" style="margin-top:14px">
      <h2>物资可用性明细与替代组合</h2>
      %s
      <div class="note" style="margin-top:10px">缺口项按同分类、同容量优先给出替代：若卡式炉具不足，可改用电磁炉（需接电营位）或共享厨房炊具。</div>
    </div>
  </div>

  <div style="width:340px;flex:0 0 340px">
    <div class="card"><h2>人工调整</h2>
      <div class="hint" style="line-height:22px;margin-bottom:10px">业务负责人可调整最终结论，但原始计算结果不覆盖，调整记录单独留痕。</div>
      <label class="f" style="align-items:flex-start"><span>调整结果</span>
        <select style="width:200px"><option>推荐</option><option>有条件推荐</option><option>不推荐</option></select></label>
      <label class="f" style="align-items:flex-start"><span>原因</span>
        <textarea rows="3" style="width:200px">客人自带帐篷，物资需求减少，周转风险可忽略</textarea></label>
      <div class="hint">原因不少于 5 个字</div>
      <button class="btn primary" style="width:100%%;margin-top:10px">提交调整</button>
    </div>
    <div class="card" style="margin-top:14px"><h2>计算留痕</h2>
      <ul class="timeline" style="margin-top:10px">
        <li><b>v3 计算 · 86.5 推荐</b><div class="m">系统 · 2026-11-01 14:05:22</div></li>
        <li><b>v3 计算 · 78.0 有条件推荐</b><div class="m">系统 · 2026-11-01 11:20:08</div><div class="hint">物资库存快照不同（炉具仅剩 2）</div></li>
        <li><b>v2 计算 · 81.0 推荐</b><div class="m">系统 · 2026-10-28 16:02:41</div><div class="hint">旧规则版本，仅供追溯</div></li>
      </ul>
    </div>
    <div class="card" style="margin-top:14px"><h2>效果统计</h2>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">
        <div class="kpi" style="padding:10px"><div class="label">推荐采纳率</div><div class="value" style="font-size:20px">82.6&#37;</div></div>
        <div class="kpi" style="padding:10px"><div class="label">人工调整率</div><div class="value" style="font-size:20px">11.9&#37;</div></div>
      </div>
      <div class="hint" style="margin-top:8px">近 30 天 42 次计算，5 次人工调整，调整后未出现物资短缺投诉。</div>
    </div>
  </div>
</div>""" % (
        "".join('<div class="score-row"><span>%s <span class="hint">(%d%%)</span></span><div class="bar"><i style="width:%d%%"></i></div><span class="v">%d</span></div>'
                % (n, w, s, s) for n, w, s in [("营位时段", 20, 95), ("容纳人数", 15, 88), ("设施需求", 15, 80),
                                              ("物资库存", 25, 92), ("物资周转时间", 15, 70), ("维护状态", 10, 100)]),
        table(["物资", "需求", "库存可用", "周转缓冲", "结论", "替代组合"], [
            ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-1.jpg" style="width:36px;height:28px;object-fit:cover;border-radius:3px">四人帐篷</div>', "1", "6", "扣除 2 顶缓冲后 4 顶", tag("满足", "t-green"), "六人隧道帐（+￥60/晚）"],
            ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-3.jpg" style="width:36px;height:28px;object-fit:cover;border-radius:3px">折叠桌椅组</div>', "1", "12", "—", tag("满足", "t-green"), "蛋卷桌 + 折叠椅 ×4"],
            ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-5.jpg" style="width:36px;height:28px;object-fit:cover;border-radius:3px">卡式炉具</div>', "1", "2", "11-07 归还 8 台", tag("勉强满足", "t-amber"), "电磁炉（需接电）"],
            ['<div style="display:flex;align-items:center;gap:8px"><img src="../img/goods-7.jpg" style="width:36px;height:28px;object-fit:cover;border-radius:3px">睡袋（单人）</div>', "3", "0", "11-08 归还 6 条", tag("当前不足", "t-red"), "毛毯 ×3（免费）"],
        ]))
    return shell("联合可用性", body, "营位物资联合可用性", "manager", "<b>物资租赁</b> / 营位物资联合可用性")


# ---------------- 16 巡检任务 ----------------
@page
def p16():
    rows = [
        ["XJ20261101-0012", "A 区", "消防", "灭火器压力、疏散通道", "顾青", "2026-11-01 09:00", tag("已完成", "t-green"), "正常", "2026-11-01 09:24"],
        ["XJ20261101-0013", "B 区", "用电", "配电箱、线缆、接电桩", "顾青", "2026-11-01 10:00", tag("已发现隐患", "t-red"), "2 处", "2026-11-01 10:38"],
        ["XJ20261101-0014", "C 区", "卫生", "洗手间、垃圾清运", "唐磊", "2026-11-01 11:00", tag("进行中", "t-blue"), "—", "2026-11-01 11:12"],
        ["XJ20261101-0015", "D 区", "道路", "路面、照明、标识", "顾青", "2026-11-01 15:00", tag("待执行", "t-amber"), "—", "—"],
        ["XJ20261101-0016", "公共设施", "公共设施", "儿童沙池、共享厨房", "唐磊", "2026-11-01 16:00", tag("待执行", "t-amber"), "—", "—"],
        ["XJ20261031-0009", "A 区", "消防", "灭火器压力、疏散通道", "顾青", "2026-10-31 09:00", tag("已完成", "t-green"), "正常", "2026-10-31 09:18"],
    ]
    body = """<h1>巡检任务</h1>
<div class="sub">按营区生成消防、用电、卫生、道路与公共设施巡检任务；发现问题转隐患登记</div>
<div class="card">
  <div class="toolbar">
    <input type="date" value="2026-11-01" style="width:160px">
    <select><option>营区：全部</option></select>
    <select><option>巡检类型：全部</option><option>消防</option><option>用电</option><option>卫生</option></select>
    <select><option>状态：全部</option></select>
    <button class="btn primary">查询</button>
    <div style="flex:1"></div>
    <button class="btn">按营区批量生成</button><button class="btn primary">+ 新建任务</button>
  </div>
  %s%s
</div>
<div class="row" style="margin-top:16px">
  <div class="card" style="flex:1"><h2>今日巡检覆盖</h2>
    <div class="row" style="gap:14px">
      <div class="kpi" style="flex:1"><div class="label">应巡检</div><div class="value" style="font-size:22px">5</div></div>
      <div class="kpi" style="flex:1"><div class="label">已完成</div><div class="value" style="font-size:22px">2</div></div>
      <div class="kpi" style="flex:1"><div class="label">发现隐患</div><div class="value" style="font-size:22px;color:var(--danger)">2</div></div>
      <div class="kpi" style="flex:1"><div class="label">覆盖率</div><div class="value" style="font-size:22px">40&#37;</div></div>
    </div>
  </div>
  <div class="card" style="width:400px"><h2>巡检项模板</h2>
    <div class="hint" style="line-height:22px">消防：灭火器压力、疏散通道、明火管控<br>用电：配电箱、线缆破损、接电桩接地<br>
      卫生：洗手间、垃圾清运、炊事区油污<br>道路：路面塌陷、照明失效、夜间标识<br>公共设施：儿童沙池、共享厨房、淋浴热水</div>
  </div>
</div>""" % (table(["任务编号", "营区", "类型", "巡检项", "巡检人", "计划时间", "状态", "结果", "完成时间", "操作"], rows), pager(36))
    return shell("巡检任务", body, "巡检任务", "manager", "<b>安全运营</b> / 巡检任务")


# ---------------- 17 隐患与事件 ----------------
@page
def p17():
    rows = [
        ["YH20261101-0007", "B 区配电箱", "用电", "高", "配电箱门锁损坏，接线端子裸露", "顾青", "唐磊", "2026-11-03", tag("待整改", "t-red"), "2026-11-01 10:38"],
        ["YH20261101-0006", "B-03 平台", "设施", "中", "木平台第三块板松动", "顾青", "唐磊", "2026-11-05", tag("整改中", "t-amber"), "2026-11-01 10:22"],
        ["YH20261031-0005", "A 区溪流边", "安全", "中", "警示牌倾倒，儿童活动区围栏缺口", "顾青", "顾青", "2026-11-02", tag("待整改", "t-red"), "2026-10-31 16:10"],
        ["YH20261030-0004", "共享厨房", "卫生", "低", "灶台油污未清理", "唐磊", "唐磊", "2026-11-01", tag("已整改", "t-green"), "2026-10-30 18:20"],
        ["YH20261029-0003", "C 区路灯", "设施", "低", "C-05 至 C-06 路灯不亮", "顾青", "唐磊", "2026-11-04", tag("已整改", "t-green"), "2026-10-29 20:05"],
    ]
    body = """<h1>隐患与运营事件</h1>
<div class="sub">巡检发现的隐患单独登记责任人与整改期限；恶劣天气、噪声投诉、设备故障等运营事件另行记录</div>
<div class="card" style="margin-bottom:14px">
  <div class="toolbar" style="margin-bottom:0">
    <span class="tag t-blue">全部 12</span><span class="tag t-red">待整改 3</span><span class="tag t-amber">整改中 2</span><span class="tag t-green">已整改 7</span>
    <div style="flex:1"></div>
    <select><option>级别：全部</option></select><button class="btn">导出</button><button class="btn primary">+ 登记隐患</button>
  </div>
</div>
<div class="card">
  %s%s
</div>
<div class="row" style="margin-top:16px">
  <div class="card" style="flex:1"><h2>运营事件登记</h2>
    %s
  </div>
  <div class="card" style="width:360px"><h2>整改跟踪</h2>
    <ul class="timeline" style="margin-top:10px">
      <li><b>YH20261101-0007 待整改</b><div class="m">责任人 唐磊 · 期限 11-03</div><div class="hint">已联系电工，明日上门更换门锁与端子护罩</div></li>
      <li><b>YH20261101-0006 整改中</b><div class="m">责任人 唐磊 · 期限 11-05</div><div class="hint">B-03 已置为维修中，暂停销售</div></li>
      <li><b>YH20261030-0004 已整改</b><div class="m">唐磊 · 2026-11-01 08:40 复核通过</div></li>
    </ul>
  </div>
</div>""" % (table(["编号", "位置", "类型", "级别", "问题描述", "发现人", "整改责任人", "期限", "状态", "发现时间", "操作"], rows), pager(12),
             table(["时间", "类型", "描述", "影响", "处置", "登记人"], [
                 ["11-01 13:20", "噪声投诉", "C-02 客人反映隔壁房车夜间音响过大", "1 组客人", "已上门沟通，22:00 后静音", "林悦"],
                 ["11-01 06:40", "恶劣天气", "气象台发布大风蓝色预警，阵风 7 级", "全部天幕", "通知加固，免费提供地钉", "顾青"],
                 ["10-31 21:10", "设备故障", "共享厨房第 2 灶点火失败", "2 组客人", "改用移动炉具，次日报修", "唐磊"],
             ]))
    return shell("隐患与事件", body, "隐患与事件", "manager", "<b>安全运营</b> / 隐患与事件")


# ---------------- 18 评价与黑名单 ----------------
@page
def p18():
    body = """<h1>评价与黑名单</h1>
<div class="sub">退营完成后可对环境、卫生、设施与服务评分；严重违规客户加入受限名单需经理审批</div>
<div class="row" style="align-items:flex-start">
  <div style="flex:1;min-width:0">
    <div class="card">
      <h2>客户评价</h2>
      <div class="toolbar"><select><option>评分：全部</option></select><select><option>营区：全部</option></select>
        <input class="search" placeholder="预约编号 / 客人"><button class="btn primary">查询</button></div>
      %s%s
    </div>
    <div class="card" style="margin-top:14px">
      <h2>受限客户名单</h2>
      %s
    </div>
  </div>
  <div style="width:340px;flex:0 0 340px">
    <div class="card"><h2>评分分布</h2>
      %s
      <div class="legend"><span><i style="background:#2E7D5B"></i>5 星</span><span><i style="background:#2F6B8A"></i>4 星</span>
        <span><i style="background:#9DB9CB"></i>3 星及以下</span></div>
      <div style="margin-top:12px;font-size:13px;line-height:24px">
        <div style="display:flex;justify-content:space-between"><span class="hint">综合评分</span><b>4.62</b></div>
        <div style="display:flex;justify-content:space-between"><span class="hint">环境</span><span>4.78</span></div>
        <div style="display:flex;justify-content:space-between"><span class="hint">卫生</span><span>4.51</span></div>
        <div style="display:flex;justify-content:space-between"><span class="hint">设施</span><span>4.44</span></div>
        <div style="display:flex;justify-content:space-between"><span class="hint">服务</span><span>4.75</span></div>
      </div>
    </div>
    <div class="card" style="margin-top:14px"><h2>新增受限记录</h2>
      <label class="f" style="align-items:flex-start"><span>客户</span><input value="邓伟 137****9014" style="width:200px"></label>
      <label class="f" style="align-items:flex-start"><span>原因</span>
        <select style="width:200px"><option>恶意损坏物资</option><option>多次爽约</option><option>严重违规</option></select></label>
      <label class="f" style="align-items:flex-start"><span>说明</span><textarea rows="3" style="width:200px">10-30 预约取消后未到店，累计爽约 3 次</textarea></label>
      <div class="note">提交后进入经理审批，审批通过才生效；解除同样需要填写原因。</div>
      <button class="btn primary" style="width:100%%;margin-top:10px">提交审批</button>
    </div>
  </div>
</div>""" % (
        table(["预约编号", "客人", "营位", "环境", "卫生", "设施", "服务", "内容", "时间"], [
            ["YY20261029-0021", "邵敏", "A-05", "5.0", "4.5", "4.5", "5.0", "营位很安静，前台响应快", "2026-10-31 10:12"],
            ["YY20261027-0014", "钱宇", "B-02", "4.5", "4.0", "4.0", "4.5", "木平台很稳，洗手间稍远", "2026-10-29 09:30"],
            ["YY20261026-0011", "孙萌", "C-03", "5.0", "5.0", "4.5", "5.0", "房车位水电方便，孩子玩沙池很开心", "2026-10-28 11:05"],
            ["YY20261024-0008", "李彤", "D-02", "4.0", "3.5", "4.0", "4.5", "林间位蚊虫较多，建议提供驱蚊", "2026-10-26 16:40"],
        ]), pager(31),
        table(["客户", "原因", "登记时间", "状态", "审批人", "操作"], [
            ["邓伟 137****9014", "累计爽约 3 次", "2026-10-31 09:12", tag("待审批", "t-amber"), "—", '<a class="btn sm">审批</a>'],
            ["郑凯 135****7720", "恶意损坏帐篷", "2026-09-18 14:20", tag("已生效", "t-red"), "顾青", '<a class="btn sm">解除</a>'],
            ["吴倩 139****3388", "多次违规明火", "2026-08-02 10:05", tag("已解除", "t-green"), "顾青", '<a class="btn sm">查看</a>'],
        ]),
        bars([21, 8, 2], ["#2E7D5B", "#2F6B8A", "#9DB9CB"], 110))
    return shell("评价与黑名单", body, "评价与黑名单", "manager", "<b>客户服务</b> / 评价与黑名单")


# ---------------- 19 规则配置 ----------------
@page
def p19():
    factors = [("营位时段", 20, "营位在入住日期区间内的空闲程度与冲突预约数", "空闲比例越高得分越高"),
               ("容纳人数", 15, "入住人数与营位标准/最大容量的匹配度", "超过标准容量按比例扣分"),
               ("设施需求", 15, "客人勾选的设施需求与营位配套的覆盖度", "缺一项按权重扣分"),
               ("物资库存", 25, "物资需求数量与扣除周转缓冲后可用量之比", "可用为 0 时该项为 0"),
               ("物资周转时间", 15, "归还与发放之间的间隔小时数", "间隔 ≥ 4 小时得满分"),
               ("维护状态", 10, "营位与所需物资的维修/停用状态", "存在维修项时该项为 0")]
    rows = [["%d" % (i + 1), n, w, d, r, tag("启用", "t-green"), '<a class="btn sm">编辑</a>']
            for i, (n, w, d, r) in enumerate(factors)]
    body = """<h1>规则配置 · 营位与租赁物资联合可用性</h1>
<div class="sub">权重合计必须等于 100%%；修改后生成新版本并记录生效时间，只影响新计算或主动重算的数据</div>
<div class="row" style="align-items:flex-start">
  <div style="flex:1;min-width:0">
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
        <h2 style="margin:0">当前版本 v3（生效 2026-10-15 00:00:00）</h2>
        <div><span class="tag t-green">权重合计 100&#37;</span></div>
      </div>
      %s
      <div class="toolbar" style="margin-top:12px"><span class="hint">修改权重后需点击「保存为新版本」，历史计算结果不受影响</span>
        <div style="flex:1"></div><button class="btn">重置</button><button class="btn primary">保存为新版本</button></div>
    </div>

    <div class="card" style="margin-top:14px">
      <h2>结果分级阈值</h2>
      %s
      <div class="hint" style="margin-top:8px">阈值同样按版本管理，修改后需重新计算才能生效。</div>
    </div>

    <div class="card" style="margin-top:14px">
      <h2>版本记录</h2>
      %s
    </div>
  </div>
  <div style="width:340px;flex:0 0 340px">
    <div class="card"><h2>缺失数据处理</h2>
      <div class="hint" style="line-height:22px">某项输入缺失时，该项不参与计算，总分按剩余权重归一化，并在结果页明确提示「需补录」。
        若缺失项为物资库存且需求非空，则直接判为「不推荐」并提示补充物资信息。</div>
    </div>
    <div class="card" style="margin-top:14px"><h2>效果统计</h2>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">
        <div class="kpi" style="padding:10px"><div class="label">近 30 天计算</div><div class="value" style="font-size:20px">42</div></div>
        <div class="kpi" style="padding:10px"><div class="label">推荐占比</div><div class="value" style="font-size:20px">42.9&#37;</div></div>
        <div class="kpi" style="padding:10px"><div class="label">采纳率</div><div class="value" style="font-size:20px">82.6&#37;</div></div>
        <div class="kpi" style="padding:10px"><div class="label">人工调整率</div><div class="value" style="font-size:20px">11.9&#37;</div></div>
      </div>
      <div class="hint" style="margin-top:8px">采纳后出现物资短缺的工单数：0</div>
    </div>
    <div class="card" style="margin-top:14px"><h2>测试与一致性</h2>
      <div class="hint" style="line-height:22px">同输入同版本必得同结果，已验证 32 条样本；覆盖正常、边界（库存刚好为 0、人数等于最大容量）与异常（营位维修中）三类。</div>
    </div>
  </div>
</div>""" % (
        table(["序号", "因素", "权重(%)", "取值说明", "计分规则", "状态", "操作"], rows),
        table(["等级", "总分区间", "说明", "建议动作"], [
            ["推荐", "≥ 80", "营位与物资均可满足，周转充裕", "直接确认预约"],
            ["有条件推荐", "60 – 79", "存在轻微缺口或周转偏紧", "提示替代组合后确认"],
            ["不推荐", "< 60", "存在硬性不满足项", "更换营位或调整物资需求"],
        ]),
        table(["版本", "生效时间", "权重摘要", "创建人", "状态"], [
            ["v3", "2026-10-15 00:00:00", "20/15/15/25/15/10", "顾青", tag("当前", "t-green")],
            ["v2", "2026-08-01 00:00:00", "25/15/10/25/15/10", "顾青", tag("历史", "t-gray")],
            ["v1", "2026-06-10 00:00:00", "25/20/15/20/10/10", "顾青", tag("历史", "t-gray")],
        ]))
    return shell("规则配置", body, "规则配置", "manager", "<b>配置</b> / 规则配置")


# ---------------- 20 用户与角色 ----------------
@page
def p20():
    rows = [
        ["1", "admin", "系统管理员", "总部", "全部数据", tag("正常", "t-green"), "2026-11-01 09:12", '<a class="btn sm">编辑</a>'],
        ["2", "gujing", "运营经理", "运营部", "全部营区", tag("正常", "t-green"), "2026-11-01 13:40", '<a class="btn sm">编辑</a>'],
        ["3", "linyue", "前台人员", "前台", "预约与入住", tag("正常", "t-green"), "2026-11-01 14:05", '<a class="btn sm">编辑</a>'],
        ["4", "tanglei", "仓库人员", "仓储", "物资与备货", tag("正常", "t-green"), "2026-11-01 08:50", '<a class="btn sm">编辑</a>'],
        ["5", "zhouwei", "前台人员", "前台", "预约与入住", tag("锁定", "t-red"), "2026-10-31 21:10", '<a class="btn sm">编辑</a>'],
        ["6", "chenke", "游客", "外部", "本人数据", tag("正常", "t-green"), "2026-10-30 16:20", '<a class="btn sm">编辑</a>'],
    ]
    body = """<h1>用户与角色</h1>
<div class="sub">账号、角色与菜单权限；菜单按角色动态下发，数据范围独立控制</div>
<div class="row" style="align-items:flex-start">
  <div style="flex:1;min-width:0">
    <div class="card">
      <div class="toolbar">
        <input class="search" placeholder="账号 / 姓名">
        <select><option>角色：全部</option></select>
        <button class="btn primary">查询</button>
        <div style="flex:1"></div><button class="btn primary">+ 新增用户</button>
      </div>
      %s%s
    </div>
    <div class="card" style="margin-top:14px">
      <h2>角色列表</h2>
      %s
    </div>
  </div>
  <div style="width:360px;flex:0 0 360px">
    <div class="card"><h2>为「前台人员」分配菜单</h2>
      <div class="hint" style="margin-bottom:10px">勾选后该角色登录可见对应菜单；菜单权限不等于接口权限</div>
      <div style="font-size:13px;line-height:26px">
        <label style="display:flex;gap:8px;align-items:center"><input type="checkbox" checked style="width:14px;height:14px">工作台 · 今日值班</label>
        <label style="display:flex;gap:8px;align-items:center"><input type="checkbox" checked style="width:14px;height:14px">预约业务 · 预约管理</label>
        <label style="display:flex;gap:8px;align-items:center"><input type="checkbox" checked style="width:14px;height:14px">预约业务 · 预约日历</label>
        <label style="display:flex;gap:8px;align-items:center"><input type="checkbox" checked style="width:14px;height:14px">预约业务 · 入住核验</label>
        <label style="display:flex;gap:8px;align-items:center"><input type="checkbox" checked style="width:14px;height:14px">预约业务 · 退营结算</label>
        <label style="display:flex;gap:8px;align-items:center"><input type="checkbox" checked style="width:14px;height:14px">客户 · 评价与黑名单</label>
        <label style="display:flex;gap:8px;align-items:center"><input type="checkbox" style="width:14px;height:14px">物资租赁 · 物资档案</label>
        <label style="display:flex;gap:8px;align-items:center"><input type="checkbox" style="width:14px;height:14px">配置 · 规则配置</label>
      </div>
      <button class="btn primary" style="width:100%%;margin-top:12px">保存分配</button>
    </div>
    <div class="card" style="margin-top:14px"><h2>数据范围</h2>
      <div class="hint" style="line-height:22px">游客仅本人预约；前台查看当日全营区；仓库仅物资相关；运营经理全部营区；管理员按授权组织范围。</div>
    </div>
  </div>
</div>""" % (table(["ID", "账号", "角色", "部门", "数据范围", "状态", "最近登录", "操作"], rows), pager(18),
             table(["角色", "编码", "人数", "菜单数", "说明"], [
                 ["系统管理员", "admin", "1", "全部（隐藏菜单管理项）", "账号与系统配置"],
                 ["运营经理", "manager", "2", "16", "营地资源、规则、审批与统计"],
                 ["前台人员", "reception", "3", "7", "预约、入住、结算"],
                 ["仓库人员", "warehouse", "2", "4", "物资、备货、发放归还"],
                 ["游客", "guest", "1", "门户页", "查询与本人预约"],
             ]))
    return shell("用户与角色", body, "用户与角色", "admin", "<b>系统</b> / 用户与角色")


# ---------------- 21 消息中心 ----------------
@page
def p21():
    rows = [
        [tag("待办", "t-red"), "新预约待确认 YY20261101-0042", "陈晓雯 · A-07 · 11-07 至 11-09", "预约详情", "2026-11-01 13:42"],
        [tag("预警", "t-amber"), "物资库存不足：睡袋（单人）", "11-07 需 12 条，当前可用 0 条", "物资档案", "2026-11-01 09:10"],
        [tag("待办", "t-red"), "隐患待整改：B 区配电箱门锁损坏", "级别：高 · 期限 2026-11-03", "隐患详情", "2026-11-01 10:38"],
        [tag("提醒", "t-blue"), "预约即将超时未确认", "YY20261031-0031 待备货已 20 小时", "备货单", "2026-11-01 08:00"],
        [tag("系统", "t-gray"), "规则版本 v3 已生效", "权重 20/15/15/25/15/10，合计 100%", "规则配置", "2026-10-15 00:00"],
    ]
    body = """<h1>消息中心</h1>
<div class="sub">待审批、即将超时、已退回、待验收与处理完成的消息；消息包含业务编号与详情跳转</div>
<div class="card">
  <div class="toolbar">
    <span class="tag t-blue">全部 18</span><span class="tag t-red">待办 6</span><span class="tag t-amber">预警 4</span><span class="tag t-gray">系统 8</span>
    <div style="flex:1"></div><button class="btn sm">全部标记已读</button>
  </div>
  %s%s
</div>""" % (table(["类型", "标题", "摘要", "跳转", "时间"], rows), pager(18))
    return shell("消息中心", body, "消息中心", "manager", "<b>配置</b> / 消息中心")


# ---------------- 22 个人中心 ----------------
@page
def p22():
    body = """<h1>个人中心</h1>
<div class="sub">修改资料、密码与头像；头像上传后右上角立即同步</div>
<div class="row" style="align-items:flex-start">
  <div style="width:340px;flex:0 0 340px">
    <div class="card" style="text-align:center">
      <img src="../img/avatar-1.jpg" style="width:84px;height:84px;border-radius:50%%;object-fit:cover;border:1px solid var(--line)" alt="头像">
      <div style="font-size:16px;font-weight:600;margin-top:10px">顾青</div>
      <div class="hint">运营经理 · 运营部</div>
      <div style="margin-top:12px"><button class="btn sm">更换头像</button></div>
      <div class="hint" style="margin-top:8px">支持 jpg/png，小于 2MB，建议 400×400</div>
    </div>
    <div class="card" style="margin-top:14px"><h2>账号信息</h2>
      %s
    </div>
  </div>
  <div style="flex:1;min-width:0">
    <div class="card">
      <h2>基本资料</h2>
      <div class="row" style="gap:24px">
        <div style="flex:1">
          <label class="f"><span>姓名</span><input value="顾青" style="width:100%%"></label>
          <label class="f"><span>手机号</span><input value="137 0021 8890" style="width:100%%"></label>
          <label class="f"><span>邮箱</span><input value="guqing@yunqi.camp" style="width:100%%"></label>
        </div>
        <div style="flex:1">
          <label class="f"><span>所属部门</span><input value="运营部" style="width:100%%" disabled></label>
          <label class="f"><span>角色</span><input value="运营经理" style="width:100%%" disabled></label>
          <label class="f"><span>负责营区</span><select style="width:100%%"><option>全部营区</option></select></label>
        </div>
      </div>
      <div style="display:flex;justify-content:flex-end;gap:8px"><button class="btn">取消</button><button class="btn primary">保存资料</button></div>
    </div>
    <div class="card" style="margin-top:14px">
      <h2>修改密码</h2>
      <div class="row" style="gap:24px">
        <div style="flex:1">
          <label class="f"><span>当前密码</span><input type="password" value="********" style="width:100%%"></label>
          <label class="f"><span>新密码</span><input type="password" value="" style="width:100%%"></label>
          <label class="f"><span>确认新密码</span><input type="password" value="" style="width:100%%"></label>
        </div>
        <div style="flex:1">
          <div class="note">密码要求：8 位以上，包含字母与数字；保存后使用 BCrypt 存储，其他会话需重新登录。</div>
        </div>
      </div>
      <div style="display:flex;justify-content:flex-end"><button class="btn primary">修改密码</button></div>
    </div>
  </div>
</div>""" % kv([("账号", "guqing"), ("角色", "运营经理"), ("最近登录", "2026-11-01 13:40:12"), ("登录 IP", "10.12.3.41"),
                ("创建时间", "2025-03-12")])
    return shell("个人中心", body, "个人中心", "manager", "<b>配置</b> / 个人中心")


# ---------------- 23 移动端 ----------------
@page
def p23():
    return """<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8">
<title>移动端巡检 · 云栖露营地</title><link rel="stylesheet" href="../mock.css"><style>
body{background:#E9EDF2;padding:28px 0}
.phone .pbody{overflow:hidden}
.mtabs{display:flex;gap:6px;margin-bottom:12px;font-size:12px}
.mtabs span{padding:5px 10px;border-radius:14px;background:#fff;border:1px solid var(--line);color:var(--text-sub)}
.mtabs span.on{background:var(--primary);color:#fff;border-color:var(--primary)}
.field2{background:#fff;border:1px solid var(--line);border-radius:8px;padding:10px 12px;margin-bottom:10px}
.field2 .lb{font-size:12px;color:var(--text-sub);margin-bottom:4px}
.photos{display:flex;gap:8px;margin-top:8px}
.photos img,.photos .add{width:64px;height:64px;border-radius:6px;object-fit:cover}
.photos .add{border:1px dashed var(--line);display:grid;place-items:center;color:var(--text-sub);font-size:20px;background:#F8FAFB}
</style></head><body>
<div class="phone">
  <div class="phead">巡检 · B 区</div>
  <div class="pbody">
    <div class="mtabs"><span class="on">待执行 3</span><span>进行中 1</span><span>已完成 2</span></div>
    <div class="mcard">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <div class="t">XJ20261101-0014 · 卫生</div><span class="tag t-blue">进行中</span></div>
      <div class="m">C 区 · 洗手间、垃圾清运 · 计划 11:00</div>
    </div>
    <div class="mcard">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <div class="t">XJ20261101-0015 · 道路</div><span class="tag t-amber">待执行</span></div>
      <div class="m">D 区 · 路面、照明、标识 · 计划 15:00</div>
    </div>
    <div class="field2"><div class="lb">巡检结果</div>
      <div style="display:flex;gap:8px"><span class="tag t-green">正常</span><span class="tag t-red">发现隐患</span><span class="tag t-gray">无法执行</span></div></div>
    <div class="field2"><div class="lb">问题级别</div>
      <div style="display:flex;gap:8px"><span class="tag t-red">高</span><span class="tag t-amber">中</span><span class="tag t-gray">低</span></div></div>
    <div class="field2"><div class="lb">现场照片（已拍 2 张）</div>
      <div class="photos"><img src="../img/patrol-1.jpg" alt="现场"><img src="../img/patrol-2.jpg" alt="现场"><div class="add">+</div></div></div>
    <div class="field2"><div class="lb">问题描述</div>
      <div style="font-size:13px;color:var(--text-sub);line-height:20px">D-03 至 D-04 之间路面有轻微塌陷，夜间照明不足。</div></div>
  </div>
  <div class="pfoot"><button class="btn" style="flex:1;height:40px">保存草稿</button><button class="btn primary" style="flex:2;height:40px">提交巡检结果</button></div>
</div>
</body></html>"""


if __name__ == "__main__":
    for name, fn in sorted(PAGES.items()):
        num = name[1:]
        title = None
        html = fn()
        if title is None:
            pass
        path = os.path.join(OUT, "%s_%s.html" % (num, {
            "01": "login", "02": "dashboard", "03": "portal", "04": "site_map", "05": "site_detail",
            "06": "site_admin", "07": "price", "08": "booking_new", "09": "booking_list",
            "10": "booking_calendar", "11": "arrival", "12": "checkout", "13": "goods",
            "14": "picklist", "15": "combo", "16": "patrol", "17": "event", "18": "review",
            "19": "rule", "20": "system", "21": "message", "22": "profile", "23": "mobile"}[num]))
        with open(path, "w", encoding="utf-8") as f:
            f.write(html)
        print("wrote", os.path.basename(path), len(html))
