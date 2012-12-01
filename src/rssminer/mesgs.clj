(ns rssminer.mesgs)

(def messages {:m-logo-title ["tip: show subscription list when you hover the logo" "提示：hover显示订阅列表"]
               :m-signup-warn ["signup to save subscriptions, get personal recommendation" "注册后可添加自定义源，得到个性化推荐"]
               :m-demo-account ["this is demo account" "这是一个测试用户"]
               :m-search-placeholder ["search feed, subscription..." "搜索订阅或文章，快速直达..."]
               :m-change-avata ["Change your avatar at gravatar.com" "点击到gravatar.com更改头像"]
               ;; app.tpl menu
               :m-add-sub ["Add subscription" "添加订阅"]
               :m-keyboard-shortcut ["Keyboard shortcuts" "键盘快捷键"]
               :m-settings ["Settings" "设置"]
               :m-search ["Search" "搜索"]
               :m-help ["Help" "帮助"]
               :m-logout ["Logout" "退出"]

               ;; app.tpl
               :m-k-next ["Next item" "上一篇文章或者上一个订阅"]
               :m-k-prev ["Previous item" "下一篇文章或者下一个订阅"]
               :m-k-open ["Open first item / Open current in new tab" "在新窗口打开"]
               :m-k-scroll-down ["Scroll down article" "向下滚屏"]
               :m-k-scroll-up ["Scroll up article" "向上滚屏"]
               :m-k-return-list ["Return to list" "回到列表"]
               :m-k-focus-tab ["Focus next tab" "选择另外一个标签"]
               :m-k-focus-search ["Focus search" "焦点移到搜索框"]
               :m-k-show-help ["Bring up this help dialog" "打开这个帮助文档"]
               :m-k-go-home ["Go home" "回到首页"]
               :m-close ["Close" "关闭"]
               :m-k-close-cancel ["Close or cancel" "取消或者关闭"]})

(def zh-messages (into {} (map (fn [[k [en zh]]] [k zh]) messages)))

(def en-messages (into {} (map (fn [[k [en zh]]] [k en]) messages)))
