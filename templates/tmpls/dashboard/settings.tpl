<ul id="charts">
  <li><div id="crawler-pie"></div></li>
  <li><div id="line-chart"></div></li>
</ul>
<ul>
  <li class="section">
    <h3>statistics</h3>
    <table>
      <tr>
        <th></th>
        <th>Count</th>
        <th>Delta</th>
      </tr>
      <tr>
        <td>Feed:</td>
        <td>{{feeds}}</td>
        <td>{{feeds_delta}}</td>
      </tr>
      <tr>
        <td>Rss:</td>
        <td>{{rss_links}} </td>
        <td>{{rss_links_delta}}</td>
      </tr>
      <tr>
        <td>Links:</td>
        <td>{{crawler_links}}</td>
        <td>{{crawler_links_delta}}</td>
      </tr>
      <tr>
        <td>Crawler counter:</td>
        <td>{{crawler_counter}}</td>
        <td>{{crawler_counter_delta}}</td>
      </tr>
      <tr>
        <td>Crawler pending:</td>
        <td>{{crawler_pending}}</td>
        <td>{{crawler_pending_delta}}</td>
      </tr>
      <tr>
        <td>Crawler speed:</td>
        <td>{{crawler_speed}}</td>
        <td>{{crawler_speed_delta}}</td>
      </tr>
      <tr>
        <td>Crawler started:</td>
        <td colspan="2">{{interval crawler_start}}</td>
      </tr>
    </table>
  </li>
  <li class="section">
    <h3>server control</h3>
    <table>
      <tr class="crawler">
        <td>Cralwer</td>
        <td>{{crawler_running}} </td>
        <td>
          <button>
            {{#crawler_running}}stop {{/crawler_running}}
              {{^crawler_running}}start {{/crawler_running}}
          </button>
        </td>
      </tr>
      <tr class="fetcher">
        <td>Fetcher</td>
        <td>{{fetcher_running}} </td>
        <td>
          <button>
            {{#fetcher_running}}stop {{/fetcher_running}}
              {{^fetcher_running}}start {{/fetcher_running}}
          </button>
        </td>
      </tr>
    </table>
  </li>
  <li class="section">
    <h3>black domain</h3> <input />
    <ul>
      {{#black_domains}}
        <li>{{patten}}</li>
      {{/black_domains}}
    </ul>
  </li>
  <li class="section">
    <h3>reseted domain</h3>
    <input/>
    <ul>
      {{#reseted_domains}}
        <li>{{patten}}</li>
      {{/reseted_domains}}
    </ul>
  </li>
</ul>


