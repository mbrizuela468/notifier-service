namespace NotifierService.Models.WhatsApp
{
    public class TwilioIncomingMessage
    {
        public string From { get; set; }   // Número del remitente
        public string Body { get; set; }   // Texto del mensaje
    }
}
