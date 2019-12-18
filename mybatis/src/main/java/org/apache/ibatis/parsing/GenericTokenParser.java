/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.parsing;

/**
 * @author Clinton Begin
 */
public class GenericTokenParser {

  private final String openToken;
  private final String closeToken;
  private final TokenHandler handler;

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

  public String parse(String text) {
    // 用来记录解析后的字符串
    final StringBuilder builder = new StringBuilder();
    // 用来记录一个占位符的字面值
    final StringBuilder expression = new StringBuilder();
    if (text != null && text.length() > 0) {
      char[] src = text.toCharArray();
      int offset = 0;
      // search open token
      // 开始查找标记
      int start = text.indexOf(openToken, offset);
      while (start > -1) {
        if (start > 0 && src[start - 1] == '\\') {
          // this open token is escaped. remove the backslash and continue.
          // 遇到转义的开始标记,则直接将前面的字符串以及开始标记追加到builder中
          builder.append(src, offset, start - offset - 1).append(openToken);
          offset = start + openToken.length();
        } else {
          // found open token. let's search close token.
          expression.setLength(0);
          // 将前面的字符串追加到builder中
          builder.append(src, offset, start - offset);
          offset = start + openToken.length();
          // 从offset向后继续查找结束标记
          int end = text.indexOf(closeToken, offset);
          while (end > -1) {
            if (end > offset && src[end - 1] == '\\') {
              // this close token is escaped. remove the backslash and continue.
              // 处理转移的结束标记
              expression.append(src, offset, end - offset - 1).append(closeToken);
              offset = end + closeToken.length();
              end = text.indexOf(closeToken, offset);
            } else {
              // 将开始标记和结束标记之间的字符串追加到expression中保存
              expression.append(src, offset, end - offset);
              offset = end + closeToken.length();
              break;
            }
          }
          // 未查找到结束标记
          if (end == -1) {
            // close token was not found.
            builder.append(src, start, src.length - start);
            offset = src.length;
          } else {
            // 将占位符的字面值交给TokenHandler处理,并将处理结果追加到builder中保存,最终拼凑出解析后的完整内容
            builder.append(handler.handleToken(expression.toString()));
            offset = end + closeToken.length();
          }
        }
        start = text.indexOf(openToken, offset);
      }
      if (offset < src.length) {
        builder.append(src, offset, src.length - offset);
      }
    }
    return builder.toString();
  }
}
