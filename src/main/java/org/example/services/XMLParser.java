package org.example.services;

import org.example.objects.PetriNet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;

public class XMLParser {

    public static PetriNet loadPetriNet(File file) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(PetriNet.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return (PetriNet) unmarshaller.unmarshal(file);
    }
}
