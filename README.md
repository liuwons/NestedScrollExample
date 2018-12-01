# Nested Scroll样例

本工程包含一个含有 title bar， WebView， footer 的样例页面，用于显示如何使用Android Support包提供的 **Nested Scroll** 接口解决滑动冲突问题。

页面的上划效果为：先收起title bar，然后WebView向上滑动直至不能滑动，然后footer上滑显示，看上去像是 title bar， WebView， footer 都在一个竖向排版的`LinearLayout`中。

[点此下载apk运行](https://github.com/liuwons/NestedScrollExample/releases/download/v1.0.0/nested_scroll_example_v1.0.0.apk)

![运行效果](./nested_scroll.gif)
