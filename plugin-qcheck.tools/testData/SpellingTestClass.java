/*
 *-----------------------------------------------------------------
 * Copyright 2018 Trent Hoeppner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *-----------------------------------------------------------------
 */

import com.ibm.commerce.qcheck.core.Tag;

/**
 * Constructor <sample>word 'fr'</sample> CommentDescription {@link cs} based on tag fragments.
 * Test spelling. <p> This phrase
 *  os duplicated.
 * 
 * 
 * 
 * <p>
 * I am heer. You are heer too. Come. Go to www.ibm.com.
 * 
 * @param fragments
 *            The list of {@link ASTNode}s that make up the description, as
 *            returned by {@link TagElement#fragments()}. Each fragment will
 *            be created usin
 *            {@link CommentFragment#createFragment(CompilationUnit, ASTNode, boolean)}
 *            on each ASTNode in this list. Cannot be null.
 * @param trimLeadingWhiteSpace
 *            True indicates that whitespace leading up to the first
 *            non-whitespace character in the first fragment will be
 *            stripped, false indicates that it will not be stripped.
 * @throws HelloException
 *            The obect that contains this object. May be null if this
 *            has no parent.
 *            
 */
public class TestPlugin {
 	
	//kdfsf
	
	/**
     * Returns the <code>@return</code> tag for this comment.
     * 
     *  @param comp
     *       The compiled form of the fil with all ASTNodes and line
     *       numbers available. Cannot be null.
     * @return The <code>@return</code> tag. Will be null if thre is no such
     *         tag.
     */
    public Tag getReturnTag(ASTNode ast) {
        Tag returnTag = null;
        for (Tag tag : tags) {
            String tagName = tag.getName().getText();
            if (tagName.equals("@return")) {
                returnTag = tag;
                break;
            }
        }

        return returnTag;
    }
    
    
    /**
     * This is <code>exempt</code>, and so is this
     * <code><samp>exempt</samp></code>.  Here are more problems and not:
     * <code>exempt<samp>exempt</code>not exempt</samp>.
     */
    public void getValidators() {
    }
}
