import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.Node;
import java .io .*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;


public class RetrieveENAData {
	/* This class takes a required command line argument providing an ENA identifier
	 * The respective metadata and genomic nucleotide sequence in FASTA format is retrieved via the ENA web API
	 * The optional information that can be output to the screen is the following:
	 * 1) description, 2) publication, 3) nucleotide sequence, 4) taxonomic division, 5) length of sequence, 6) sequence in FASTA format,  
	 * 7) all of the above data
	 * 
	 * Some example parameter inputs are:
	 * a) -i AF163095 -a
	 * b) -i AF163095 -f
	 * c) -i AF163095 -d -s
	 * d) -i BN000065 -d -p 
	 * e) -i BN000065 -d -l -m 
	 * 
	 */

	public static void main(String[] args) throws ParserConfigurationException, SAXException {
		
		//command line parameters for identifier (mandatory) and metadata (optional)
		Options options = new Options();
		Option input = new Option("i", "identifier", true , "ENA identifier");		
		Option description = new Option("d", "description", false, "Description");
		Option publication = new Option("p", "publication", false, "Supporting publication");
		Option sequence = new Option("s", "sequence", false, "Sequence");
		Option taxonomicDivision = new Option("t", "taxonomy", false, "Taxonomic division");
		Option moleculeType = new Option("m", "molecule", false, "Molecule type");
		Option sequenceLength = new Option("l", "length", false, "Sequence length");
		Option all = new Option("a", "all", false, "Display all relevant metadata");
		Option fasta = new Option("f", "fasta", false, "Display FASTA sequence");
		Option help = new Option("h", "help", false, "Display help");
		
		input.setRequired(true);
		
		options.addOption(input);
		options.addOption(description);
		options.addOption(publication);
		options.addOption(sequence);
		options.addOption(taxonomicDivision);
		options.addOption(moleculeType);
		options.addOption(sequenceLength);
		options.addOption(all);
		options.addOption(fasta);
		options.addOption(help);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("Enter a mandatory ENA identifier in the following format, ex: -i AF163095. \n"
					+ " If you would like to display additional information, please enter desired parameters from below. \n"
					+ "An example input is: -i AF163095 -a "
					+ "", options);
			System.exit(1);
			return;
		}
		
		String identifier = cmd.getOptionValue("identifier");
		System.out.println("The ENA identifier is : " + identifier);		
		//display help
		if (cmd.hasOption("h")){
		System.out.println("For the ENA identifier you have entered, additional information that can be displayed is the following : \n"
				+ "1. description (-d), 2. publication (-p), 3. nucleotide sequence (-n), 4. taxonomic division (-t), \n"
				+ "5. length of sequence (-l), 6. sequence in FASTA format (-f) 7. all of the above data (-a). \n"
				+ "An example parameter input is: -i BN000065 -d -l -m ");
		}

		File responseFile = new File("response.xml");

		//API call performed using a CloseableHttpClient object
		CloseableHttpClient httpclient = HttpClients.createDefault();
		//object can execute a query with an object of type HttpGet
		//Retrieve results by a single identifier 
		HttpGet httpGet = new HttpGet("https://www.ebi.ac.uk/ena/data/view/" + identifier + "&display=xml&download=xml&filename=" + identifier + ".xml");		
		HttpResponse response;

		try {
			response = httpclient.execute(httpGet);
			HttpEntity result = (response.getEntity());
			//write to xml file
			if (result != null) {
				try (FileOutputStream outstream = new FileOutputStream("response.xml")) {
					result.writeTo(outstream);
				}
			}
			try {
				//parse XML and build a document which can be used to evaluate the XML content:
				DocumentBuilderFactory dfactory = DocumentBuilderFactory .newInstance ();
				DocumentBuilder builder = dfactory .newDocumentBuilder ();
				Document doc = builder.parse("response.xml");
				doc.getDocumentElement().normalize();

				if (cmd.hasOption("d")){
					String desc = doc.getElementsByTagName("description").item(0).getTextContent();
					System.out.println("Description : " + desc);
				}
				if (cmd.hasOption("p")){
					String title = doc. getElementsByTagName("title").item(0).getTextContent();
					System.out.println("Publication : " + title);
				}
				if (cmd.hasOption("s")){
					String seq = doc. getElementsByTagName("sequence").item(0).getTextContent();
					System.out.println("Sequence : " + seq); 
				}

				//Return a new NodeList object (nlist) containing all elements in document with tag name 'entry'
				NodeList nList = doc.getElementsByTagName("entry"); 
				//iterate over all child elements in the nodelist for node entry
				for (int i = 0; i < nList.getLength(); i++) {
					org.w3c.dom.Element nNode =  (org.w3c.dom.Element) nList.item(i);

					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						org.w3c.dom.Element eElement =  nNode;
						if (cmd.hasOption("t")){
							System.out.println("Taxonomic Division : " + (eElement).getAttribute("taxonomicDivision"));
						}
						if (cmd.hasOption("m")){
							System.out.println("Type of Molecule : " + (eElement).getAttribute("moleculeType"));
						}
						if (cmd.hasOption("l")){
							System.out.println("Sequence Length : " + (eElement).getAttribute("sequenceLength"));
						}
					}	
				}
			} catch (SAXException e) {
				// handle SAXException 
			} catch (IOException e) {
				// handle IOException 
			}
		} catch (ClientProtocolException e) {
			e. printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (cmd.hasOption("a")){
			getMetadata(identifier);

		}
		if (cmd.hasOption("f")){
			getFASTAseq(identifier);
		}
	}

	public static void getFASTAseq(String identifier) throws ParserConfigurationException, SAXException{
		//retrieve the FASTA sequence of the ENA identifier specified
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet("http://www.ebi.ac.uk/ena/data/view/" + identifier + "&display=fasta&download=fasta&filename=" + identifier + ".fasta.");
		HttpResponse response;
		try {
			response = httpclient.execute(httpGet);
			String result = EntityUtils.toString(response.getEntity());
			System.out.println("FASTA sequence : \n" +  result);

		} catch (ClientProtocolException e) {
			e. printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void getMetadata(String identifier) throws ParserConfigurationException, SAXException{
		//retrieve all of the relevant metadata
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet("https://www.ebi.ac.uk/ena/data/view/" + identifier + "&display=xml&download=xml&filename=" + identifier + ".xml");		
		HttpResponse response;
		try {
			response = httpclient.execute(httpGet);
			HttpEntity result = (response.getEntity());
			if (result != null) {
				try (FileOutputStream outstream = new FileOutputStream("response.xml")) {
					result.writeTo(outstream);
				}
			}
			try {
				DocumentBuilderFactory dfactory = DocumentBuilderFactory .newInstance ();
				DocumentBuilder builder = dfactory .newDocumentBuilder ();

				Document doc = builder.parse("response.xml");
				doc.getDocumentElement().normalize();

				String desc = doc. getElementsByTagName ("description"). item (0). getTextContent ();
				System.out.println("Description : " + desc);
				String title = doc. getElementsByTagName ("title"). item (0). getTextContent ();
				System.out.println("Publication : " + title);
				String seq = doc. getElementsByTagName ("sequence"). item (0). getTextContent ();
				System.out.println("Sequence : " + seq);

				NodeList nList = doc.getElementsByTagName("entry");

				for (int temp = 0; temp < nList.getLength(); temp++) {
					org.w3c.dom.Element nNode =  (org.w3c.dom.Element) nList.item(temp);

					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						org.w3c.dom.Element eElement =  nNode ;
						System.out.println("Taxonomic Division : " + (eElement).getAttribute("taxonomicDivision"));
						System.out.println("Type of Molecule : " + (eElement).getAttribute("moleculeType"));
						System.out.println("Sequence Length : " + (eElement).getAttribute("sequenceLength"));						
					}	
				}
			} catch (SAXException e) {
				// handle SAXException 
			} catch (IOException e) {
				// handle IOException 
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}
		} catch (ClientProtocolException e) {
			e. printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		getFASTAseq(identifier);
	}
	
}
