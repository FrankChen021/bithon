<#--

       Copyright 2020 bithon.org

       Licensed under the Apache License, Version 2.0 (the "License");
       you may not use this file except in compliance with the License.
       You may obtain a copy of the License at

           http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing, software
       distributed under the License is distributed on an "AS IS" BASIS,
       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
       See the License for the specific language governing permissions and
       limitations under the License.

-->
<#if title?? >
## ${title}
</#if>
<#list sections as section>
<#if section.title != "">
### ${section.title}
</#if>
<#list section.properties as prop>
<#if prop.type == "separator">
---
<#elseif prop.type == "list">
- ${prop.text}
<#elseif prop.type == "quote">
> ${prop.text}
<#elseif prop.type == "property">
    <#if prop.name?? >
#### **${prop.name}**: ${prop.value}
    </#if>
<#elseif prop.type == "text">
${prop.text}
</#if>
</#list>
</#list>
