package com.believersresource.api.v1;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import com.believersresource.api.CachedData;
import com.believersresource.data.BiblePassage;
import com.believersresource.data.BiblePassages;
import com.believersresource.data.BibleVerse;
import com.believersresource.data.BibleVerses;
import com.believersresource.data.RelatedPassage;
import com.believersresource.data.RelatedTopic;
import com.believersresource.data.TextSearch;
import com.believersresource.data.Topic;
import com.believersresource.data.Topics;
import com.believersresource.data.Utils;
import com.believersresource.data.Vote;
import com.google.gson.Gson;

@ManagedBean(name="defaultBean")
@RequestScoped
public class DefaultBean {

	public String getOutput() { return output; }
	int translationId = 1;
	private String output = "";
	
	public DefaultBean()
	{
		HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
		if (request.getParameter("translationId")!=null) translationId=Integer.parseInt(request.getParameter("translationId"));
		String action = request.getParameter("action");
		

        if (action.equals("addtopic")) addTopic();
        else if (action.equals("addpassage")) addPassage();
        else if (action.equals("listen")) listen();
        else if (action.equals("search")) search();
        else if (action.equals("vote")) vote();
	}
	
	private void addPassage()
    {
		HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String contentType = request.getParameter("contentType");
        int contentId = Integer.parseInt(request.getParameter("contentId"));
        int passageId = Integer.parseInt(request.getParameter("passageId"));
        int ipAddress = Utils.getIntForIP(request.getRemoteAddr());

        RelatedPassage rp = RelatedPassage.load(contentType, contentId, passageId);
        if (rp == null)
        {
            rp = new RelatedPassage();
            rp.setContentType(contentType);
            rp.setContentId(contentId);
            rp.setPassageId(passageId);
            rp.setVotes(0);
            rp.save();
        }
        int userId = 0;
        Vote.cast("relatedpassage", rp.getId(), ipAddress, userId, true);
    }
	
    private void addTopic()
    {
    	HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String topic = request.getParameter("topic");
        String contentType = request.getParameter("contentType");
        int contentId = Integer.parseInt(request.getParameter("contentId"));
        int ipAddress = Utils.getIntForIP(request.getRemoteAddr());
        
        //System.Text.RegularExpressions.Match match = System.Text.RegularExpressions.Regex.Match(topic, "[A-Za-z]{3,99}");
        //if (match == null || match.Value != topic) return;

        String baseWord = Topic.getBaseWord(topic);
        Topic t = Topic.loadByBaseWord(baseWord);
        if (t == null)
        {
            t = new Topic();
            t.setBaseWord(baseWord);
            t.setName(topic);
            t.setUrl(t.getName().toLowerCase() + ".html");
            t.save();
        }

        RelatedTopic rt = RelatedTopic.load(contentType, contentId, t.getId());
        if (rt == null)
        {
            rt = new RelatedTopic();
            rt.setContentId(contentId);
            rt.setContentType(contentType);
            rt.setTopicId(t.getId());
            rt.setVotes(0);
            rt.save();
        }
        int userId = 0;
        Vote.cast("relatedtopic", rt.getId(), ipAddress, userId, true);
    }
	
    private void vote()
    {
    	HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String contentType = request.getParameter("contentType");
        int contentId = Integer.parseInt(request.getParameter("contentId"));
        int ipAddress = Utils.getIntForIP(request.getRemoteAddr());
        boolean up = request.getParameter("up").equals("1");
    	
        Vote vote = new Vote();
        vote.setContentType(contentType);
        vote.setContentId(contentId);
        vote.setIpAddress(ipAddress);
        vote.setUserIdIsNull(true);
        vote.setVoteDate(new Date());
        if (up) vote.setPoints(1); else vote.setPoints(-1);
        output = String.valueOf(vote.cast());
    }
    
    private void listen()
    {
    	HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        int startVerseId = Integer.parseInt(request.getParameter("startVerseId"));
        int endVerseId = Integer.parseInt(request.getParameter("endVerseId"));

        BibleVerses verses = BibleVerses.loadRange(startVerseId, endVerseId);
        ArrayList<Object> result = new ArrayList<Object>();
        for (BibleVerse verse : verses)
        {
            String url="http://downloads.believersresource.com/content/audiobibles/web/" + String.format("%02d", verse.getBookId()) + "/" + String.format("%03d", verse.getChapterNumber()) + "/" + String.format("%03d", verse.getVerseNumber()) + ".mp3";
            Hashtable<String, String> ht = new Hashtable<String, String>();
            ht.put("mp3", url);
            result.add(ht);
        }
        Gson gson = new Gson();
        output = gson.toJson(result);
    }
    
    private void search()
    {
    	HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String term = request.getParameter("term").trim();
        
        Hashtable<String, Object> result = new Hashtable<String, Object>();

        BiblePassage passage = BiblePassage.parse(term, CachedData.getBibleBooks(), false);

        if (passage!=null)
        {
            appendPassage(result, passage);
        }
        else
        {
            if (term.split(" ").length == 1)
            {
                appendTopic(result, term);
            }
            else
            {
                int verseId = TextSearch.findMatch(term);
                passage = BiblePassage.load(verseId, verseId);
                appendPassage(result, passage);
            }
        }
        
        Gson gson = new Gson();
        output = gson.toJson(result);
    }
    
    private void appendTopic(Hashtable<String, Object> result, String term)
    {
        if (!term.contains(" "))
        {
            String baseWord = Topic.getBaseWord(term);
            Topic topic = Topic.loadByBaseWord(baseWord);

            if (topic != null)
            {
            	/*
                if (topic.getOpenBible() == false)
                {
                    try
                    {
                        OpenBibleHelper.ParseTopic(topic, CachedData.BibleBooks);
                    }
                    catch { }
                    topic.OpenBible = true;
                    BelieversResourceLib.Topic.SaveTopic(topic);
                }
				*/
                Hashtable<String, Object> tHash = new Hashtable<String, Object>();
                tHash.put("name", topic.getName());

                BiblePassages rps = BiblePassages.loadForTopic(topic.getId(), 20);
                rps = rps.consolidateAndUpdate();
                rps.populateVerses(translationId);

                ArrayList<Object> rpArray = rps.toList();
                tHash.put("relatedPassages", rpArray);
                result.put("topic", tHash);
            }
        }
    }
    
    
    

    private void appendPassage(Hashtable<String, Object> result, BiblePassage inputPassage)
    {
        BiblePassage context=null;
        BiblePassages allPassages = new BiblePassages();

        BiblePassage passage = BiblePassage.load(inputPassage.getStartVerseId(), inputPassage.getEndVerseId());
        allPassages.add(passage);

        if (passage != null)
        {
            if (passage.getIdIsNull())
            {
            	passage.save();
                Topics.generateForPassage(passage.getId(), passage.getStartVerseId(), passage.getEndVerseId());
            }

            BiblePassages rps = BiblePassages.loadRelated("passage", passage.getId(), 20);
            allPassages.addAll(rps);

            Topics topics = Topics.loadRelated("passage", passage.getId(), 10);
            

            if (passage.getStartVerseId() == passage.getEndVerseId())
            {
                context = BiblePassage.loadContext(passage.getStartVerseId());
                allPassages.add(context);
            }

            allPassages.populateVerses(translationId);

            Hashtable<String, Object> pHash = passage.toHash();
            ArrayList<Object> rpArray = rps.toList();
            ArrayList<Object> topicArray = topics.toList();

            if (context != null) pHash.put("context", context.toHash());
            pHash.put("relatedPassages", rpArray);
            pHash.put("relatedTopics", topicArray);
            result.put("passage", pHash);

        }
    }
    
    
	
}
