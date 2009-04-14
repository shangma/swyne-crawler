package edu.iit.swyne.crawler.experiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import edu.iit.swyne.crawler.NewsDocument;
import edu.iit.swyne.crawler.extractor.TextToTagExtractor;

public class Experiment {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws JDOMException, IOException {
		if (args.length < 2) {
			printUsage();
			return;
		}
		
		Document corpusXML = (new SAXBuilder()).build(new File(args[0]));
		Element documents = corpusXML.getRootElement();
		Iterator<Content> news = documents.getChildren().iterator();
		
		while (news.hasNext()) {
			Content content = (Content) news.next();
			if (content instanceof Element) {
				Element doc = (Element) content;
				
				NewsDocument n = new NewsDocument();
				n.setTitle(doc.getChildText("title"));
				n.setCollection(doc.getChildText("collection"));
				n.setSource(doc.getChildText("source"));
				String articleBody = doc.getChildText("article");
				if (articleBody == null || articleBody.equals("")) {
					System.err.println("Hard coded extraction failed on: " + n.getSource());
					continue;
				}
				n.setArticle(articleBody);
				
				try {
					DateFormat f = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
					Date d = f.parse(doc.getChildText("date"));
					n.setPublishedDate(d);
				} catch (ParseException e) {
					System.err.println("Could not successfully parse date for article: "+ n.getSource());
					e.printStackTrace();
				}
				
				System.out.println(n.getSource());
				
				URL docURL = new URL(n.getSource());
				File html = new File(new File(args[1], n.getCollection()), Integer.toBinaryString(docURL.hashCode())+".html");

				try {
					String htmlText = "", line;
					BufferedReader in = new BufferedReader(new FileReader(html));
					
					while ((line = in.readLine()) != null) {
						htmlText += line+"\n";
					}
					
					TextToTagExtractor t = new TextToTagExtractor(docURL, n.getTitle(), n.getPublishedDate(), n.getCollection(), htmlText);
					NewsDocument nPrime = t.parseArticle();
					
					Metric m = new Metric(n.getArticle(), nPrime.getArticle());
					
					System.out.println(m.getControlLength() + ", " + m.getExperimentalLength());
					System.out.println(m.getPrecision() + ", " + m.getRecall());
				}
				catch (IOException e) {
					System.err.println(e.getMessage());
				}
			}
		}
	}

	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("Experiment <corpusXML> <corpus_directory>");
		System.out.println();
	}
}
