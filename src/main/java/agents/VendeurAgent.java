package agents;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;
import jade.wrapper.ControllerException;

import java.util.Random;

public class VendeurAgent extends GuiAgent {

    protected VendeurGui vendeurGui;

    @Override
    protected void setup() {
        if (getArguments().length == 1) {
            vendeurGui = (VendeurGui) getArguments()[0];
            vendeurGui.vendeurAgent = this;
        }
        ParallelBehaviour parallelBehaviour = new ParallelBehaviour();
        addBehaviour(parallelBehaviour);

        parallelBehaviour.addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                // Enregister le service de vente de livres
                DFAgentDescription dfd = new DFAgentDescription();
                dfd.setName(getAID());
                ServiceDescription sd = new ServiceDescription();
                sd.setType("transaction");
                sd.setName("Vente_livres");
                dfd.addServices(sd);
                try {
                    DFService.register(myAgent, dfd);
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        });
        parallelBehaviour.addSubBehaviour(new CyclicBehaviour() {
            ///loger le message dans l'interface
            @Override
            public void action() {
                ACLMessage aclMessage = receive();
                if (aclMessage != null) {
                    //afficher le msg apres reception
                    vendeurGui.logMessage(aclMessage);
                    switch (aclMessage.getPerformative()) {
                        case ACLMessage.CFP:
                            ACLMessage reply=aclMessage.createReply();
                            // changer l'acte de la réponse
                            reply.setPerformative(ACLMessage.PROPOSE);
                            // prix aleatoire
                            reply.setContent(String.valueOf(500+new Random().nextInt(1000)));
                            send(reply);
                            break;
                        // accepter la proposition
                        case ACLMessage.ACCEPT_PROPOSAL:
                            ACLMessage aclMessage1=aclMessage.createReply();
                            // accepter  l'offre
                            aclMessage1.setPerformative(ACLMessage.AGREE);
                            aclMessage1.setContent(aclMessage.getContent());
                            send(aclMessage1);

                            break;
                    }
                } else block();
            }
        });
    }

    // Avant que l'agent soit détruit
    @Override
    protected void takeDown() {
        try {
            // Suppression d’un service publié
            //désenregistrer l'agent dans les services
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    @Override
    protected void beforeMove() {
        try {
            System.out.println("Avant migration de l'agent: " + this.getAID().getName());
            System.out.println("de conteneur: " + this.getContainerController().getContainerName());
        } catch (ControllerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void afterMove() {
        try {
            System.out.println("Apres migration de l'agent: " + this.getAID().getName());
            System.out.println("vers conteneur: " + this.getContainerController().getContainerName());
        } catch (ControllerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuiEvent(GuiEvent guiEvent) {
        //type achat si 1 q'on a passer on parameter dans class ConsomaContainer - button.setOnAction
        if (guiEvent.getType() == 1) {
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
