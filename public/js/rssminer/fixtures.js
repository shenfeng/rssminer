(function(){
  // var favicon = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABmJLR0QA/w//A/+gv6+TAAAACXBIWXMAAABIAAAASABGyWs+AAAACXZwQWcAAAAQAAAAEABcx6/DAAAC/UlEQVQ4yz/Qy2+UZR+G8+t53//72hlKOw+ZwXZa5GAbU8/QegxJNW/hSj/ow8RIg4+4kQVqT7owbH/lR++Kh41/gC41b9+QNAE8JB+0Ug9tp0IrtF+md/p978lFj6trd++XW757t7lzo9k8nW/5GRUZEUVEBV/4v7JdVBAhB5/a9+52P15dWj5vB5/N05+8Mi/e10USh4RqQ41Br6+ZoB/UCmIEVRFU9//22SkAm2/5GS/LejY4S//x56/sEF//I6/Ow+YKqgE1BlFQ4T9RpC+Xe7+v90+rIiMig6/Ro//kS0heI/+Cd6+N//MiY+5Lu7+AK4gRA4JLhD+iKn/M288Mn7MKml/EQ+/eQWwPe98wpj+OaR1F3AZ025AC0UMoI6FMOCdiRQ+1h9+5QX/pU1QDp6+G3/c028Mvo7tG0WNnC9Uab+ZzQl/gi0QoEzEo2w+ayEYeoT/5Bn/XiEtXi//f4G5ewT/2G7//OH/0F//PO/+nL7YFDlJKWB/Ug++tYBoHk95xe/BZ48pV448XC5c+I8+IGX0OO3ES5m/xS9+JXkhGM+8cHz+X54+ED9/tX6/zF1+to41Dy+4jh5/XcH/fo/cfwT/HiFtdtn+9TH+Rq4+Kgm+Kl+/Ex4+EHy/gv3+P255G+4cxE68T7m1Rf/8ZRE8+N4/sHCIUgRRBRQU1o5UdZ4++jD12Fk/x058QV3/DDB5G90+yNT+Nv7+Iq7cwg4cIL5JCQkVAN6+NM8/EK8xDJ9+DL+/+n8/9/B+IY/c9h+9s4G5cR2+G3T1Kj7ItQC+pU8Rvw+Ytk+sS15/xmy/l4gxxcx/tax/JcC+LEC+6o0ZKBhJJl99/Y7/WyB5QT8/+A0Rbx7Vn8+trx6+Ga40Ti5/u7G+0fxA7OE+xsk+5cA+tmQ/5/9/Db+0a65/qyVM9FA+/FQgOQg+hj4/CE10ie9l+3k/EG8xAz5ozxQ/ml7/pKmI3v9+D3k+/8yI+CCEKU4RoD8layB/psWg1S1I1b6/lh5/VW+//B8/fW4sq07/PAAAAJXRFWHRkYXRlOmNyZWF0ZQAyMDExLTA2LTAzVDA5OjM1OjExKzA4OjAwjnIAhgAAACV0RVh0ZGF0ZTptb2RpZnkAMjAxMS0wNi0wM1QwOTozNDozOCswODowM8dQk6oAAAAASUVOR65CY4I=";

  var favicon = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAOCAYAAAAfSC3RAAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAABIAAAASABGyWs+AAAACXZwQWcAAAAOAAAADgCxW/H3AAACXklEQVQoz02QPWiddRjFf8/z/N/33tvbq71JhfQjik2M34MhLQhaKVZBF3EIjsHJdnJycGm1LuLoIHRwEFwcFNRF1IBQsAYpQkuCVFpoytUSbEzpvfS++X88DongcDjTOZzzk8tvPXSo1++fC6JviNAVE9QEUUEMRAXddXdG94bxi9uDv8+EXr9/rsaWRCuz2lAfo5rQSrHK0Ao07JaZdDt7O0vu+wlBbJEYbe+JN+k8/SJ5c508+I2yfhEZDf4LICYAKNnanWoxiNKzSrB2G5ucJkzNwZMvU+4MyKtfUn7/BslDQCjRSeMChZ78eWbBKzJ2X5+wb5Kw/0GquRPYkeNI3aVcXyavfEze2iA1TmqcZltQUdBKaD32Aq35RaRqkVY+If7wLn77KjrzEnrsbRJd0jiTxoWSHBUTNAjhwKPUzyzSfuUD6lc/QgTST+9TNtaw2ZPY46+Txk5uCiU79s7Jw+/VNTC8iW9cQRT0wDw2/Sz5xq/EaxcJM8fRB2YZr10g/bOJq6KqoJUiwwE++IV04UPypfPQuh87dprtWzdoVn/E9h2kmn2O1OT/TTXHjjxP/dp5wtFTxCtfk/5YxqaeQA8tcO/yMnihfniB4jVefBeOCTb1FDIxAwePkqLSrH4PODY9T7y1Th5uYpOHIeyhZCe4c1eC9sq173BtEwdrOz/GV5GVr4h/XSdubTH8+VvcoaSC1HJXbp6d/3RiqrOkniyNyw7yxsmNk7edvF12FMEL2J4qe9c/s6VHqktoPVGyzcXodYxCKkJBKQiuigdFWoZ2wsjb/vlodOfsv+CuDYS3FBkqAAAAJXRFWHRkYXRlOmNyZWF0ZQAyMDExLTA2LTAzVDA5OjMwOjE5KzA4OjAwW7SFpQAAACV0RVh0ZGF0ZTptb2RpZnkAMjAwOS0wNy0xOFQwMzo0MzoxMiswODowMPgSDTcAAAAASUVORK5CYII=";

  // var favicon = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==";

  var unread_count = [
    {
      group_name : 'blog',
      subscriptions:[
        {
          id: 1,   // subscription-id
          total_count: 100,
          unread_count: 10,
          title: "Peter Norvig",
          favicon: favicon
        },
        {
          id: 6,
          total_count: 301,
          unread_count: 2,
          title: "ScottGu's Blog",
          favicon: favicon
        }
      ]
    },
    {
      group_name: 'clojure',
      subscriptions:[
        {
          id: 611,   // subscription-id
          total_count: 10,
          unread_count: 0,
          title: "Clojure Norvig",
          favicon: favicon
        },
        {
          id: 10,
          total_count: 31,
          unread_count: 10,
          title: "Rich Hickey's Blog",
          favicon: favicon
        }
      ]
    }];
  var feeds = {
    "id":1,
    "title":"ScottGu's Blog ",
    "description":"Scott Guthrie lives in Seattle and builds a few products for Microsoft",
    "alternate":null,
    "updated_ts":null,
    "continuation":3,
    "items":[
      {"id":1,
       "author":"ScottGu",
       "title":"Free \u201cGuathon\u201d all day event in London on June 6th",
       "summary":"<p>The (awesome) UK developer community is holding another <a href=\"http:\/\/developerdeveloperdeveloper.com\/gulon2\/\" target=\"_blank\">all day event with Steve Sanderson and me in London<\/a> on June 6th.&#160; <\/p>  <p>The event is free to attend, and the venue will be in Central London (at the ODEON Covent Garden).&#160; The website for the event is <a href=\"http:\/\/developerdeveloperdeveloper.com\/gulon2\/\" target=\"_blank\">here<\/a>.<\/p>  <h2><u>Content<\/u><\/h2>  <p>The event goes from 9am to 5pm, and will feature a bunch of great .NET content.&#160; The current <a href=\"http:\/\/developerdeveloperdeveloper.com\/gulon2\/Schedule.aspx\" target=\"_blank\">agenda<\/a> includes the following talks:<\/p>  <p><u><strong>Build an app using ASP.NET MVC 3, EF Code First, NuGet and IIS Express (ScottGu)<\/strong><\/u><\/p>  <p>We'll spend 2 hours building an application with some of the latest releases of the Microsoft Web Stack. You get to choose what app to build and then watch Scott code it on stage. See how the Microsoft web stack fits together, how to take advantage of great new features, and learn some tips and tricks you might not know about along the way.<\/p>  <p><strong><u>Dynamic Web UIs with Knockout.js (Steve Sanderson)<\/u><\/strong><\/p>  <p>Building rich user experiences with JavaScript can be difficult. In this session you'll learn how Knockout.js lets you use the Model-View-ViewModel (MVVM) pattern to combine clean object-oriented code with simple declarative bindings to create sophisticated UIs that run in any mainstream browser (including mobile ones).&#160; Knockout.js works great with jQuery, and enables you to build even richer web experiences.<\/p>  <p><strong><u>C# 5 and Async Web Applications with ASP.NET vNext (Steve Sanderson)<\/u><\/strong><\/p>  <p>This session explores how asynchronous web programming will get much easier in ASP.NET vNext, and how you could use this to boost performance. We'll consider techniques for going beyond Ajax and efficiently pushing updates from server to client in near-realtime.<\/p>  <p><u><strong>Cloud Development (ScottGu)<\/strong><\/u><\/p>  <p>Scott is now spending a lot of time working on Windows Azure - Microsoft's Cloud Platform.&#160; In this session he will cover why cloud computing will be a huge opportunity for .NET developers, and provide an inside look into how the Windows Azure Platform works.&#160; He'll discuss some of the architecture considerations involved in running large scale services, how to design for elastic scale and fault tolerance, and how you'll be able to take advantage of the Windows Azure Platform to deliver even better solutions.<\/p>  <h2><u>Register for Free<\/u><\/h2>  <p>You can register to attend this all day event for free <a href=\"http:\/\/developerdeveloperdeveloper.com\/gulon2\/Register.aspx\" target=\"_blank\">here<\/a>.<\/p>  <p>Hope to see some of you there!<\/p>  <p>Scott<\/p>  <p>P.S. In addition to blogging, I am also now using Twitter for quick updates and to share links. Follow me at: <a href=\"http:\/\/www.twitter.com\/scottgu\" target=\"_blank\">twitter.com\/scottgu<\/a><\/p><img src=\"http:\/\/weblogs.asp.net\/aggbug.aspx?PostID=7801606\" width=\"1\" height=\"1\">",
       "alternate":"http:\/\/weblogs.asp.net\/scottgu\/archive\/2011\/05\/23\/free-guathon-all-day-event-in-london-on-june-6th.aspx",
       "published_ts":"Tue, 24 May 2011 10:15:16 +0800",
       "categories":[
         {
           "type":"tag",
           "text":"ASP.NET",
           "added_ts":"Sat, 04 Jun 2011 15:15:43 +0800"
         },
         {
           "type":"tag",
           "text":".NET",
           "added_ts":"Sat, 04 Jun 2011 15:15:43 +0800"
         }],
       "comments":[
         {
           id: 100101,
           added_ts: "Tue, 24 May 2011 09:51:13 -0400",
           content: "this is a test comemnt"
         }
       ]
      },
      {
        "id":2,
        "author":"ScottGu",
        "title":"Upcoming Conference talks in Norway, Germany and the UK",
        "summary":"<p>Next month I\u2019ll be in Europe giving presentations at some great .NET conferences.&#160;&#160; Below are details on the three conferences I\u2019m presenting at: <\/p>  <h2><u>Norwegian Developers Conference (NDC 2011)<\/u><\/h2>  <p>I\u2019ll be in Oslo, Norway for the <a href=\"http:\/\/www.ndc2011.no\/\" target=\"_blank\">NDC 2011 conference<\/a> (June 8th to 10th).&#160; I\u2019ve heard really great things about NDC \u2013 I\u2019m excited to be able to finally attend in person!&#160; I\u2019m doing a keynote talk, two breakout talks, and an unplugged Q&amp;A session.<\/p>  <p>Details on NDC can be found <a href=\"http:\/\/www.ndc2011.no\/\" target=\"_blank\">here<\/a>.<\/p>  <h2><u>Microsoft DevConnections Germany<\/u><\/h2>  <p>I\u2019ll be in Karlsruhe, Germany for the <a href=\"http:\/\/www.devconnections.com\/shows\/europe2011\/default.aspx?s=175\" target=\"_blank\">Microsoft DevConnections Germany conference<\/a> (June 8th to 10th). I\u2019m doing a keynote talk on the 10th (Friday).&#160; <\/p>  <p>Details on Microsoft DevConnections Germany can be found <a href=\"http:\/\/www.devconnections.com\/shows\/europe2011\/default.aspx?s=175\" target=\"_blank\">here<\/a>.<\/p>  <h2><u>Microsoft DevConnections UK<\/u><\/h2>  <p>I\u2019ll be in London, England for the <a href=\"http:\/\/www.devconnections.com\/shows\/europe2011\/default.aspx?s=174\" target=\"_blank\">Microsoft DevConnections UK conference<\/a> (June 13th to 15th).&#160; I\u2019m doing a keynote talk on the 14th (Tuesday). <\/p>  <p>Details on Microsoft DevConnections UK can be found <a href=\"http:\/\/www.devconnections.com\/shows\/europe2011\/default.aspx?s=174\" target=\"_blank\">here<\/a>.<\/p>  <p>Hope to see some of you at one of these events!<\/p>  <p>Scott<\/p>  <p>P.S. In addition to blogging, I am also now using Twitter for quick updates and to share links. Follow me at: <a href=\"http:\/\/www.twitter.com\/scottgu\" target=\"_blank\">twitter.com\/scottgu<\/a><\/p><img src=\"http:\/\/weblogs.asp.net\/aggbug.aspx?PostID=7793966\" width=\"1\" height=\"1\">",
        "alternate":"http:\/\/weblogs.asp.net\/scottgu\/archive\/2011\/05\/16\/upcoming-conference-talks-in-norway-germany-and-the-uk.aspx",
        "published_ts":"Tue, 17 May 2011 14:09:31 +0800",
        "categories":[
          {
            "type":"tag",
            "text":"ASP.NET",
            "added_ts":"Sat, 04 Jun 2011 15:15:43 +0800"
          },
          {
            "type":"tag",
            "text":"Community News",
            "added_ts":"Sat, 04 Jun 2011 15:15:43 +0800"
          }],
        "comments":[
          {
            id: 100101,
            added_ts: "Tue, 24 May 2011 09:51:13 -0400",
            content: "this is a test comemnt"
          }]
        }]};
  window.Rssminer = $.extend(window.Rssminer, {
    fixtures: {
      unread_count: unread_count,
      feeds: feeds
    }});
})();
