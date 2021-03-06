package fr.ign.validator.process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

import fr.ign.validator.Context;
import fr.ign.validator.ValidatorListener;
import fr.ign.validator.model.AttributeType;
import fr.ign.validator.model.Document;
import fr.ign.validator.model.DocumentFile;
import fr.ign.validator.model.FeatureType;
import fr.ign.validator.model.FileModel;
import fr.ign.validator.model.file.MetadataModel;
import fr.ign.validator.model.file.PdfModel;
import fr.ign.validator.model.file.TableModel;
import fr.ign.validator.reader.InvalidCharsetException;
import fr.ign.validator.tools.TableReader;

/**
 * 
 * Produit un répertoire DATA dans le dossier validation en normalisant les données en
 * fonction du modèle.
 * 
 * <ul>
 * 	<li>Les tables sont normalisées sous forme de CSV en fonction du FeatureType correspondant</li>
 *  <li>Les fichiers classiques (MetadataFile, PDF) sont copiés</li>
 *  <li>Les répertoires sont ignorés</li>
 * </ul>
 * 
 * @author MBorne
 *
 */
public class NormalizePostProcess implements ValidatorListener {
	public static final Logger log = LogManager.getRootLogger() ;
	public static final Marker MARKER = MarkerManager.getMarker("CreateNormalizedCsvPostProcess") ;
	

	@Override
	public void beforeMatching(Context context, Document document) throws Exception {
		//RAS
	}

	@Override
	public void beforeValidate(Context context, Document document) throws Exception {
		//RAS
	}

	@Override
	public void afterValidate(Context context, Document document) throws Exception {
		// Création du répertoire DATA
		File dataDirectory = context.getDataDirectory() ;
		if ( ! dataDirectory.exists() ){
			dataDirectory.mkdirs() ;
		}
		
		// Création du répertoire METADATA
		File metadataDirectory = context.getMetadataDirectory() ;
		if ( ! metadataDirectory.exists() ){
			metadataDirectory.mkdirs() ;
		}
		
		log.info(MARKER,"Création des fichiers normalisés dans {}",dataDirectory);
		List<DocumentFile> documentFiles = document.getDocumentFiles() ;
		for (DocumentFile documentFile : documentFiles) {
			File srcFile = documentFile.getPath() ;
			FileModel fileModel = documentFile.getFileModel() ;
			
			if ( fileModel instanceof TableModel ){
				// Création d'un fichier CSV standardisé
				File csvFile = new File(dataDirectory, fileModel.getName()+".csv" ) ;
				{
					log.info(MARKER,"Normalisation de {} en {}...",srcFile, csvFile);
					createNormalizedCSV(
						fileModel.getFeatureType(),
						srcFile,
						csvFile,
						context.getEncoding(),
						context.getCoordinateReferenceSystem()
					);						
				}
			}else if ( fileModel instanceof PdfModel ){
				File destFile = new File(dataDirectory, srcFile.getName()) ;
				log.info(MARKER,"Copie de {} dans {}...",srcFile, destFile);
				FileUtils.copyFile(srcFile, destFile);
			}else if ( fileModel instanceof MetadataModel ){
				File destFile = new File(metadataDirectory, srcFile.getName()) ;
				log.info(MARKER,"Copie de {} dans {}...",srcFile, destFile);
				FileUtils.copyFile(srcFile, destFile);
			}
		}
	}
	
	/**
	 * Creation d'un fichier CSV normalisé
	 * @param featureType
	 * @param srcFile
	 * @param destFile
	 * @throws IOException
	 * @throws FactoryException 
	 * @throws NoSuchAuthorityCodeException 
	 * @throws TransformException 
	 * @throws MismatchedDimensionException 
	 */
	private void createNormalizedCSV(
		FeatureType featureType,
		File srcFile, 
		File destFile, 
		Charset charset,
		CoordinateReferenceSystem sourceCRS
	) throws IOException, NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException, TransformException {
		CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");
		MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
		
		/*
		 * Creation d'un fichier temporaire contenant un CSV normalisé {{destFile}}.csv
		 */
		TableReader reader = null ;
		try {
			reader = TableReader.createTableReader(srcFile,charset);
		} catch (InvalidCharsetException e1) {
			log.error(MARKER,"Détection de la charset pour la normalisation de {}", srcFile);
			reader = new TableReader(srcFile) ;
		}
		
		
		String[] inputHeader = reader.getHeader() ;

		String[] outputHeader = featureType.getAttributeNames() ;
		BufferedWriter fileWriter = new BufferedWriter(
			new OutputStreamWriter(new FileOutputStream(destFile), StandardCharsets.UTF_8)
		);
		CSVPrinter printer = new CSVPrinter(fileWriter, CSVFormat.RFC4180) ;
		printer.printRecord(outputHeader);
			
		/*
		 * écriture de chaque Feature
		 */
		while ( reader.hasNext() ){
			String[] inputRow = reader.next() ;
			String[] outputRow = new String[featureType.getAttributeCount()];
			for ( int i = 0; i < inputRow.length; i++ ){
				int position = featureType.indexOf(inputHeader[i]) ;
				if ( position < 0 ){
					continue ;
				}
				// binding
				AttributeType<?> attribute = featureType.getAttribute(position) ;
				Object bindedValue = null;
				try {
					bindedValue = attribute.bind(inputRow[i]);
					if ( bindedValue instanceof Geometry ){
						bindedValue = JTS.transform( (Geometry)bindedValue, transform);
					}
				}catch (IllegalArgumentException e){
					log.warn(MARKER,
						"{}.{} : {} transformé en valeur nulle",
						inputRow[i],
						featureType.getName(),
						attribute.getName()
					);
				}
				// formatage
				outputRow[position] = attribute.formatObject(bindedValue) ;
			}
			printer.printRecord(outputRow);
		}
		
		printer.close();
	}


	
	
}
