# Glance はウィジェットのアクションをクラス名からリフレクションで生成する
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    <init>();
}
