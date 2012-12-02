var _MESGS_ = {
  'm_unread_dislike': ["dislike count", "未读数目"],
  'm_neutral_count': ["neutral count", "猜你可能喜欢文章数"],
  'm_like_count': ["like count", "猜你喜欢文章数"],
  'm_like_title': ["I like it, give me more like this in recommend tab", "我喜欢这篇文章"],
  'm_dislike_title': ["I like it, give me more like this in recommend tab", "不喜欢这篇文章，不要为我推荐类似的文章"],
  'm_no_entries': ["No entries", "这里暂时没有文章"],
  'm_publish': ["Publish:", "发表于："],
  'm_read': ["Read:", "阅读于："],
  // settings
  'm_import': ['Import', '导入'],
  'm_url': ['URL', '直接添加'],
  'm_add': ['Add', '添加'],
  'm_paste_url': ['paste atom/rss url here', '把订阅的RSS的地址粘贴在这里，点击添加'],
  'm_import_grader': ['Import all you google reader subscriptions:', "导入您的google Reader订阅列表"],
  'm_demo_add_warn': ['This is a public account, please <a href="/">create your own</a> to add subscription.', '这是一个演示帐户，请点击<a href="/">这里</a>免费创建您自己的帐户，再添加订阅'],
  'm_default_list': ['Default list: default show the automatic recommendation or just the newest', '默认显示列表：最新或者推荐'],
  'm_set_pass_p1': ['Login in with Google OpenID is encouraged. But if you are in China...', '用google帐户登陆当然最好了，那样不需要记忆额外的密码。如果你在中国，我们为您准备了'],
  'm_set_pass_p2': ['If you may have trouble login with Google OpenID. You can set a password. The user name is your email address.',
                    '如果用Google帐户登陆不是很顺利，你可以设置一个登陆密码，用户名是您的Google帐户邮箱'],
  'm_set_pass_p3': ['You can always login with Google OpenID, even after you set the password','设置了登陆密码后，就可以用登陆秘密登陆，也可以用Google OpenID登陆'],
  'm_password': ['Password', '登陆密码'],
  'm_password_again': ['Confirm password', '确认登陆密码'],
  'm_save': ['Save', '保存密码'],

  // sub_ct_menu
  'm_new_folder': ['new folder', '新建文件夹'],
  'm_unsubscribe': ['unsubscribe', '取消订阅'],
  'm_visite': ['visite', '访问'],

  // search_result
  'm_search': ["Enter search keyword", "输入关键字"],

  // tooltip
  'm_like': ["Guess you may like the content of this article",'猜测你喜欢这篇文章的内容'],
  'm_neutral': ["Guess you may like the content of this article",'猜测你可能喜欢这篇文章的内容'],
  'm_dislike': ["Guess you may not very like the content of this article",'猜测你可能不是很喜欢这篇文章的内容'],
  'm_e_like': ["You have mark this article as like",'你曾把这篇文章标记为喜欢'],
  'm_e_dislike': ["You mark this as dislike",'你曾把这篇文章标记为不喜欢']
};

function _LANG_ (k) {
  var words = {
    recommend: '推荐', newest: '最新', read: '已读过',
    voted: '喜欢过', oldest: '最旧', next: '下一页',
    add: '添加订阅', settings: '设置', help: '帮助'
  };
  if(words[k]) {
    if(_LANG_ZH_) {
      return words[k];
    } else {
      return k;
    }
  } else if(_MESGS_[k]){
    if(_LANG_ZH_) {
      return _MESGS_[k][1];
    } else {
      return _MESGS_[k][0];
    }
  }
}
