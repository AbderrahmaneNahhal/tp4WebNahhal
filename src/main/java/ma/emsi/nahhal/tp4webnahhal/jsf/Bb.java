package ma.emsi.nahhal.tp4webnahhal.jsf;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import ma.emsi.nahhal.tp4webnahhal.llm.LlmClient;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("bb")
@ViewScoped
public class Bb implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(Bb.class.getName());

    private String roleSysteme;
    private boolean roleSystemeChangeable = true;
    private String question;
    private String reponse;
    private final StringBuilder conversation = new StringBuilder();

    @Inject
    private LlmClient llmClient;

    public String envoyer() {
        if (question == null || question.isBlank()) {
            afficherErreur("Il manque le texte de la question.");
            return null;
        }
        if (roleSysteme == null || roleSysteme.isBlank()) {
            afficherErreur("Vous devez indiquer le rôle de l'API.");
            return null;
        }

        try {
            if (roleSystemeChangeable) {
                llmClient.setSystemRole(roleSysteme);
            }

            reponse = llmClient.envoyerRequete(question);
            roleSystemeChangeable = false;

            conversation.append("== User:\n")
                    .append(question)
                    .append("\n== Serveur:\n")
                    .append(reponse)
                    .append("\n");
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Échec de la requête Gemini", e);
            reponse = null;
            afficherErreur("Impossible d'obtenir une réponse de Gemini. Consultez le journal Payara.");
        }

        return null;
    }

    public String nouveauChat() {
        return "/index.xhtml?faces-redirect=true";
    }

    private void afficherErreur(String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", detail));
    }

    public String getRoleSysteme() {
        return roleSysteme;
    }

    public void setRoleSysteme(String roleSysteme) {
        this.roleSysteme = roleSysteme;
    }

    public boolean isRoleSystemeChangeable() {
        return roleSystemeChangeable;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return reponse;
    }

    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getConversation() {
        return conversation.toString();
    }

    public List<SelectItem> getRolesSysteme() {
        return List.of(
                new SelectItem("""
                        You are a helpful assistant. Answer the user's questions clearly.
                        """, "Assistant"),
                new SelectItem("""
                        Translate French text into English and English text into French.
                        Give only the translation.
                        """, "Traducteur Anglais-Français"),
                new SelectItem("""
                        You are a travel guide. For the city or country named by the user,
                        suggest places to visit and give an estimate of the price of a meal.
                        """, "Guide touristique"),
                new SelectItem("""
                        Tu es un coach en qualité des données pour un étudiant en data engineering.
                        Réponds en français simple. Repère les anomalies visibles, explique
                        leur impact et propose une règle de validation avec un exemple.
                        Si une valeur manque, indique-le sans l'inventer.
                        """, "Coach qualité des données")
        );
    }
}