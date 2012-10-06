<div id="search-result">
  <!-- <input value="{{q}}"/> -->
  <table>
    <tr>
      {{#tags.length}}<td class="name">Tags:</td>{{/tags.length}}
        <td>
          <ul class="filters">
            {{#tags}}
              <li {{#selected}}class="selected"{{/selected}}>
                <a href="#search?q={{q}}&{{filter}}&offset=0">
                  <span class="filter">{{ tag }}</span>
                  <span class="c">{{ count }}</span>
                </a>
              </li>
            {{/tags}}
          </ul>
        </td>
    </tr>
    <tr>
      {{#authors.length}}<td class="name">Authors: </td>{{/authors.length}}
        <td>
          <ul class="filters">
            {{#authors}}
              <li {{#selected}}class="selected"{{/selected}}>
                <a href="#search?q={{q}}&{{filter}}&offset=0">
                  <span class="filter">{{ author }}</span>
                  <span class="c">{{ count }}</span></a>
              </li>
            {{/authors}}
          </ul>
        </td>
    </tr>
  </table>
  <ul class="feeds">
    {{#feeds}}
      <li class="feed {{cls}}" data-id="{{id}}">
        <a href="#{{href}}">
          <span class="indicator"></span>
          <span class="title">{{{title}}}</span>
          <i class="thumbs">
            <i class="icon-thumbs-up"
              data-title="like it, give me more like this in recommend tab">
            </i>
            <i class="icon-thumbs-down"
              data-title="dislike, less in recommend tab">
            </i>
          </i>
          <span class="author"
            data-title="author">{{author}}</span>
            <span class="date">{{ date }}</span>
        </a>
      </li>
    {{/feeds}}
  </ul>
</div>
