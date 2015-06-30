<?php
class UserM extends CI_Controller {

	public function __construct()
	{
		parent::__construct();
                date_default_timezone_set('Asia/Singapore');
		$this->load->model('admintool/userA_model');
		$this->load->model('admintool/userC_model');
	}
        
        public function userA(){
            
        }
        
        public function userC(){
            
        }
    
}

