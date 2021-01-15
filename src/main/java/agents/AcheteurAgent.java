package agents;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.ControllerException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IntSummaryStatistics;
import java.util.List;

public class AcheteurAgent  extends GuiAgent {

    protected AcheteurGui acheteurGui;
    protected AID[] vendeurs;

    @Override
    protected void setup() {
        if (getArguments().length==1){
            acheteurGui = (AcheteurGui) getArguments()[0];
            acheteurGui.acheteurAgent=this;
        }
        ParallelBehaviour parallelBehaviour=new ParallelBehaviour();
        addBehaviour(parallelBehaviour);

        parallelBehaviour.addSubBehaviour(new TickerBehaviour(this, 50000) {
            @Override
            protected void onTick() {
                // Mettre à jour la liste des agents vendeurs
                DFAgentDescription dfa = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                // recherche des services par type
                sd.setType("transaction");
                // recherche des services par nom
                sd.setName("Vente_livres");
                dfa.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent,dfa);
                    // définir la liste des vendeurs (tableau)
                    vendeurs = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        vendeurs[i] = result[i].getName();
                    }
                } catch (FIPAException fe) { fe.printStackTrace(); }
            }
        });
        parallelBehaviour.addSubBehaviour(new CyclicBehaviour() {
            private int compteur=0;
            //pour stocker les réponses
            private List<ACLMessage> replies= new ArrayList<ACLMessage>();
            //loger le message dans l'interface
            @Override
            public void action() {
                // l'agent ne reçoint que les msg avec  REQUEST,PROPOSE , AGREE, REFUSE
                MessageTemplate template=
                        MessageTemplate.or(
                            MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                MessageTemplate.or(
                                        MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                                        MessageTemplate.or(
                                                MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                                                MessageTemplate.MatchPerformative(ACLMessage.REFUSE))));

                ACLMessage aclMessage=receive(template);
                if(aclMessage!=null){
                    // filter les messages
                    // getPerformative represente l'acte de communication(request, PROPOSE..)
                    switch(aclMessage.getPerformative()){
                        case ACLMessage.REQUEST :
                            // appel à proposition
                            String livre=aclMessage.getContent();
                            ACLMessage aclMessage1=new ACLMessage(ACLMessage.CFP);
                            aclMessage1.setContent(livre);
                           for (AID aid:vendeurs) {
                               aclMessage1.addReceiver(aid);
                           }
                           send(aclMessage1);
                            break;

                        case ACLMessage.PROPOSE :
                            ++compteur;
                            replies.add(aclMessage);
                            if(compteur==vendeurs.length){
                                // calculer le minimum prix
                                ACLMessage meilleureOffre=replies.get(0);
                                double min=Double.parseDouble(replies.get(0).getContent());
                                //IntSummaryStatistics summaryStatistics = replies.stream().map(aclMessage::getContent).summaryStatistics().max();
                                for(ACLMessage offre:replies){
                                    double price=Double.parseDouble(offre.getContent());
                                    System.out.println("INSIDE FOR: price: "+price+"   min: "+min+" OFFER: "+offre.getContent());
                                    if(price<=min){
                                        min=price;
                                        System.out.println("INSIDE if: price: "+price+"   min: "+min);
                                        meilleureOffre=offre;

                                    }
                                }
                                ACLMessage aclMessageAccept=meilleureOffre.createReply();
                                aclMessageAccept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                aclMessageAccept.setContent(meilleureOffre.getContent());
                                // envoyer la meilleure offre
                                System.out.println("meilleureOffre ENVOYE: "+meilleureOffre.getContent());
                                send(aclMessageAccept);
                            }

                            break;
                        case ACLMessage.AGREE:
                            ACLMessage aclMessage2=new ACLMessage(ACLMessage.CONFIRM);
                            aclMessage2.addReceiver(new AID("Consomateur",AID.ISLOCALNAME));
                            aclMessage2.setContent(aclMessage.getContent());
                            send(aclMessage2);
                            break;
                        case ACLMessage.REFUSE:
                            break;
                    }
                    String livre=aclMessage.getContent();
                    //afficher le msg
                    acheteurGui.logMessage(aclMessage);
                    //créer une réponse
                    ACLMessage reply=aclMessage.createReply();
                    reply.setContent(aclMessage.getContent());
                    send(reply);
                    ACLMessage aclMessage2=new ACLMessage(ACLMessage.CFP);
                    aclMessage2.setContent(livre);
                    // Envoyer ver le vendeur
                    aclMessage2.addReceiver(new AID("VENDEUR",AID.ISLOCALNAME));
                    send(aclMessage2);
                }else block();
            }
        });
    }

    @Override
    protected void takeDown() {
        System.out.println("L'agent : "+this.getAID().getName()+" Taken down!!! ");
    }

    @Override
    protected void beforeMove() {
        try {
            System.out.println("Avant migration de l'agent: "+this.getAID().getName());
            System.out.println("de conteneur: "+this.getContainerController().getContainerName());
        } catch (ControllerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void afterMove() {
        try {
            System.out.println("Apres migration de l'agent: "+this.getAID().getName());
            System.out.println("vers conteneur: "+this.getContainerController().getContainerName());
        } catch (ControllerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuiEvent(GuiEvent guiEvent) {
        //type achat si 1 q'on a passer on parameter dans class ConsomaContainer - button.setOnAction
        if(guiEvent.getType()==1){
            //achat
            //MESSAGE Type est request
            ACLMessage aclMessage = new ACLMessage(ACLMessage.REQUEST);
            String livre = guiEvent.getParameter(0).toString();
            aclMessage.setContent(livre);
            aclMessage.addReceiver(new AID("rma", AID.ISLOCALNAME));
            send(aclMessage);
        }
    }
}
