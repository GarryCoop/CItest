<?php
class Login_model extends CI_Model {

	public function __construct()
	{
		$this->load->database();
	}
        
        public function get_login($slug = FALSE)
        {
                if ($slug === FALSE)
                {
                        $query = $this->db->get('news');
                        return $query->result_array();
                }

                $query = $this->db->get_where('news', array('slug' => $slug));
                return $query->row_array();
        }
        
        public function check_login($name)
        {
                $this->load->helper('url');
                if($name != null && $name != ''){
                    $query = $this->db->get_where('userA', array('userA_name' => $name));
                    log_message('error','res--:'.  print_r($query->row_array(),1));
                    return $query->row_array();
                }else{
                    return $null;
                }
        }
}

